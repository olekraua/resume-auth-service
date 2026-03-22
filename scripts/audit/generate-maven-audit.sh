#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${REPO_ROOT}"

mkdir -p target/audit docs/architecture

# 1) Core Maven outputs
./mvnw -q -pl microservices/backend/services/auth-service -am -DskipTests \
  help:effective-pom -Doutput="${REPO_ROOT}/target/audit/effective-pom.xml"

./mvnw -pl microservices/backend/services/auth-service -am -DskipTests \
  dependency:tree -Dscope=runtime -Dverbose -DoutputType=text \
  -DoutputFile="${REPO_ROOT}/target/audit/dependency-tree-runtime.txt"

./mvnw -pl microservices/backend/services/auth-service -am -DskipTests \
  dependency:tree -Dscope=compile -Dverbose -DoutputType=text \
  -DoutputFile="${REPO_ROOT}/target/audit/dependency-tree-compile.txt"

./mvnw -q -pl microservices/backend/services/auth-service -am -DskipTests \
  dependency:list -DincludeScope=runtime \
  -DoutputFile="${REPO_ROOT}/target/audit/dependency-list-runtime.txt"

# 2) Direct dependencies in service and parent POMs
awk '
BEGIN { in_deps=0; g="" }
/<dependencies>/ { if (in_deps==0) { in_deps=1; next } }
in_deps==1 && /<\/dependencies>/ { in_deps=0; exit }
in_deps==1 && /<groupId>/ {
  g=$0
  sub(/.*<groupId>/, "", g)
  sub(/<\/groupId>.*/, "", g)
}
in_deps==1 && /<artifactId>/ {
  a=$0
  sub(/.*<artifactId>/, "", a)
  sub(/<\/artifactId>.*/, "", a)
  if (g!="") {
    print g ":" a
    g=""
  }
}
' microservices/backend/services/auth-service/pom.xml | sort -u > target/audit/direct-service-artifacts.txt

awk '
BEGIN { in_deps=0; g="" }
/<dependencies>/ { if (in_deps==0) { in_deps=1; next } }
in_deps==1 && /<\/dependencies>/ { in_deps=0; exit }
in_deps==1 && /<groupId>/ {
  g=$0
  sub(/.*<groupId>/, "", g)
  sub(/<\/groupId>.*/, "", g)
}
in_deps==1 && /<artifactId>/ {
  a=$0
  sub(/.*<artifactId>/, "", a)
  sub(/<\/artifactId>.*/, "", a)
  if (g!="") {
    print g ":" a
    g=""
  }
}
' pom.xml | sort -u > target/audit/direct-parent-artifacts.txt

# 3) Runtime mapping: artifact -> source -> dependency path
awk -v svc_file='target/audit/direct-service-artifacts.txt' -v parent_file='target/audit/direct-parent-artifacts.txt' '
BEGIN {
  while ((getline l < svc_file) > 0) { svc[l]=1 }
  close(svc_file)
  while ((getline l < parent_file) > 0) { par[l]=1 }
  close(parent_file)

  OFS="\t"
  print "artifact", "source", "dependency_path"
}
{
  raw=$0
  if (raw=="" || raw ~ /omitted for /) next

  s=raw
  pos1=index(s, "+- ")
  pos2=index(s, "\\- ")

  if (pos1==0 && pos2==0) {
    depth=0
  } else {
    if (pos1==0 || (pos2>0 && pos2<pos1)) pos=pos2
    else pos=pos1
    prefix=substr(s,1,pos-1)
    depth=length(prefix)/3 + 1
    s=substr(s,pos+3)
  }

  gsub(/^ +| +$/, "", s)
  if (s=="") next

  if (substr(s,1,1)=="(") {
    sub(/^\(/, "", s)
    sub(/\)$/, "", s)
  }

  sub(/ - .*/, "", s)
  gsub(/^ +| +$/, "", s)
  if (s=="") next

  n=split(s, p, ":")
  if (n < 2) next

  ga=p[1] ":" p[2]

  if (depth==0) {
    root=ga
    stack[0]=ga
    maxd=0
    next
  }

  stack[depth]=ga
  if (depth > maxd) maxd=depth
  for (i=depth+1; i<=maxd; i++) delete stack[i]

  path=root
  for (i=1; i<=depth; i++) {
    if (i in stack) path=path " -> " stack[i]
  }

  if (!(ga in seen)) {
    if (depth==1) {
      if (ga in svc) src="direct-service"
      else if (ga in par) src="inherited-parent"
      else src="direct-unknown"
    } else {
      src="transitive"
    }

    print ga, src, path
    seen[ga]=1
  }
}
' target/audit/dependency-tree-runtime.txt > target/audit/dependency-mapping-runtime.tsv

{
  echo "# Runtime Dependency Mapping (E1-T1-S1 / S1-5)"
  echo
  echo "Source file: \`target/audit/dependency-tree-runtime.txt\`"
  echo
  echo "Columns: \`artifact | source | dependency_path\`"
  echo
  echo "| artifact | source | dependency_path |"
  echo "|---|---|---|"
  awk -F '\t' 'NR>1 { gsub(/\|/, "\\|", $3); printf "| `%s` | `%s` | `%s` |\n", $1, $2, $3 }' target/audit/dependency-mapping-runtime.tsv
} > docs/architecture/maven-dependency-mapping-runtime.md

# 4) Runtime dependencies inherited from parent with effective-pom refs
awk -F '\t' 'NR>1 && $2=="inherited-parent" {print $1"\t"$3}' target/audit/dependency-mapping-runtime.tsv \
  | sort -u > target/audit/runtime-deps-from-parent.tsv

{
  echo -e "artifact\tdependency_path\teffective_pom_ref"
  while IFS=$'\t' read -r artifact dep_path; do
    gid="${artifact%%:*}"
    aid="${artifact##*:}"

    ref=$(awk -v g="$gid" -v a="$aid" '
      {
        if (index($0, "<groupId>" g "</groupId>")) gline=NR
        if (index($0, "<artifactId>" a "</artifactId>")) {
          if (gline && (NR - gline) <= 8) { print "L" gline "-L" NR; found=1; exit }
          if (!first) first=NR
        }
      }
      END {
        if (!found) {
          if (first) print "L" first
          else print "n/a"
        }
      }
    ' target/audit/effective-pom.xml)

    echo -e "${artifact}\t${dep_path}\t${ref}"
  done < target/audit/runtime-deps-from-parent.tsv
} > target/audit/runtime-deps-from-parent-with-proof.tsv

{
  echo "# Runtime Dependencies From Parent (E1-T1-S1 / S1-6)"
  echo
  echo "Source files:"
  echo "- \`target/audit/dependency-mapping-runtime.tsv\`"
  echo "- \`target/audit/effective-pom.xml\`"
  echo
  echo "| artifact | dependency_path (runtime tree) | effective-pom ref |"
  echo "|---|---|---|"
  awk -F '\t' 'NR>1 { gsub(/\|/, "\\|", $2); printf "| `%s` | `%s` | `%s` |\n", $1, $2, $3 }' target/audit/runtime-deps-from-parent-with-proof.tsv
} > docs/architecture/maven-runtime-deps-from-parent.md

# 5) Full AS-IS inventory table
awk '
BEGIN { OFS="\t" }
{
  raw=$0
  if (raw=="" || raw ~ /omitted for /) next

  s=raw
  pos1=index(s, "+- ")
  pos2=index(s, "\\- ")

  if (pos1==0 && pos2==0) {
    depth=0
  } else {
    if (pos1==0 || (pos2>0 && pos2<pos1)) pos=pos2
    else pos=pos1
    prefix=substr(s,1,pos-1)
    depth=length(prefix)/3 + 1
    s=substr(s,pos+3)
  }

  gsub(/^ +| +$/, "", s)
  if (s=="") next

  if (substr(s,1,1)=="(") {
    sub(/^\(/, "", s)
    sub(/\)$/, "", s)
  }

  sub(/ - .*/, "", s)
  sub(/ \(.*/, "", s)
  gsub(/^ +| +$/, "", s)
  if (s=="") next

  n=split(s,p,":")
  if (n<5) next

  ga=p[1]":"p[2]
  if (depth==0) next

  version=p[n-1]
  scope=p[n]

  if (!(ga in seen)) {
    print ga,version,scope
    seen[ga]=1
  }
}
' target/audit/dependency-tree-runtime.txt > target/audit/runtime-dependency-details.tsv

awk -F '\t' '
BEGIN {
  OFS="\t"
  while ((getline l < "target/audit/runtime-dependency-details.tsv") > 0) {
    split(l,a,"\t")
    details_ver[a[1]]=a[2]
    details_scope[a[1]]=a[3]
  }
  close("target/audit/runtime-dependency-details.tsv")

  while ((getline l < "target/audit/runtime-deps-from-parent-with-proof.tsv") > 0) {
    if (l ~ /^artifact\t/) continue
    split(l,a,"\t")
    parent_ref[a[1]]=a[3]
  }
  close("target/audit/runtime-deps-from-parent-with-proof.tsv")

  print "artifact","version","scope","source","introduced-by","dependency-path","effective-pom-ref","action-proposal"
}
NR==1 { next }
{
  artifact=$1
  source=$2
  dep_path=$3

  version=(artifact in details_ver ? details_ver[artifact] : "n/a")
  scope=(artifact in details_scope ? details_scope[artifact] : "n/a")

  if (source=="direct-service") {
    introduced_by="service-pom"
  } else if (source=="inherited-parent") {
    introduced_by="parent-pom"
  } else {
    split(dep_path,parts," -> ")
    if (length(parts) >= 2) introduced_by=parts[length(parts)-1]
    else introduced_by="unknown"
  }

  if (source=="direct-service" && artifact=="net.devstudy:resume-shared") {
    action="remove-umbrella-use-platform-modules"
  } else if (source=="direct-service") {
    action="keep-explicit"
  } else if (source=="inherited-parent") {
    action="move-to-explicit-service-pom"
  } else {
    action="review-after-parent-cleanup"
  }

  ref=(artifact in parent_ref ? parent_ref[artifact] : "-")

  print artifact,version,scope,source,introduced_by,dep_path,ref,action
}
' target/audit/dependency-mapping-runtime.tsv > target/audit/maven-as-is-inventory.tsv

TOTAL=$(awk 'END{print NR-1}' target/audit/maven-as-is-inventory.tsv)
DIRECT=$(awk -F '\t' 'NR>1 && $4=="direct-service"{c++} END{print c+0}' target/audit/maven-as-is-inventory.tsv)
PARENT=$(awk -F '\t' 'NR>1 && $4=="inherited-parent"{c++} END{print c+0}' target/audit/maven-as-is-inventory.tsv)
TRANSITIVE=$(awk -F '\t' 'NR>1 && $4=="transitive"{c++} END{print c+0}' target/audit/maven-as-is-inventory.tsv)

{
  echo "# Maven AS-IS Inventory (E1-T1-S1 / S1-7)"
  echo
  echo "Status: completed"
  echo
  echo "## Scope"
  echo
  echo "Runtime dependency inventory for \`resume-auth-service\` with source attribution and dependency paths."
  echo
  echo "## Inputs"
  echo
  echo "- \`target/audit/effective-pom.xml\`"
  echo "- \`target/audit/dependency-tree-runtime.txt\`"
  echo "- \`target/audit/dependency-tree-compile.txt\`"
  echo "- \`target/audit/dependency-mapping-runtime.tsv\`"
  echo "- \`target/audit/runtime-deps-from-parent-with-proof.tsv\`"
  echo
  echo "## Summary"
  echo
  echo "- Total artifacts: ${TOTAL}"
  echo "- direct-service: ${DIRECT}"
  echo "- inherited-parent: ${PARENT}"
  echo "- transitive: ${TRANSITIVE}"
  echo
  echo "## Inventory Table"
  echo
  echo "Columns: \`groupId:artifactId | version | scope | source | introduced-by | dependency-path | effective-pom-ref | action-proposal\`"
  echo
  echo "| artifact | version | scope | source | introduced-by | dependency-path | effective-pom-ref | action-proposal |"
  echo "|---|---:|---|---|---|---|---|---|"
  awk -F '\t' 'NR>1 {
    for(i=1;i<=8;i++) gsub(/\|/,"\\|",$i)
    printf "| `%s` | `%s` | `%s` | `%s` | `%s` | `%s` | `%s` | `%s` |\n",$1,$2,$3,$4,$5,$6,$7,$8
  }' target/audit/maven-as-is-inventory.tsv
  echo
  echo "## Notes"
  echo
  echo "- \`effective-pom-ref\` is populated for artifacts classified as \`inherited-parent\`."
  echo "- \`action-proposal\` is an audit hint for next tasks (E1-T2/E1-T4/E1-T5), not a final migration decision."
  echo
  echo "## How To Reproduce Audit"
  echo
  echo "Run from repository root:"
  echo
  echo "\`\`\`bash"
  echo "scripts/audit/generate-maven-audit.sh"
  echo "\`\`\`"
  echo
  echo "Verify generated artifacts:"
  echo
  echo "\`\`\`bash"
  echo "ls -lh target/audit/effective-pom.xml \\"
  echo "  target/audit/dependency-tree-runtime.txt \\"
  echo "  target/audit/dependency-tree-compile.txt \\"
  echo "  target/audit/dependency-list-runtime.txt \\"
  echo "  target/audit/dependency-mapping-runtime.tsv \\"
  echo "  target/audit/runtime-deps-from-parent-with-proof.tsv \\"
  echo "  target/audit/maven-as-is-inventory.tsv \\"
  echo "  docs/architecture/maven-runtime-deps-from-parent.md \\"
  echo "  docs/architecture/maven-as-is-inventory.md"
  echo "\`\`\`"
  echo
  echo "Note: if GitHub Packages authentication is not configured, Maven may print \`401 Unauthorized\` metadata warnings, but audit artifacts can still be generated from locally available dependencies."
} > docs/architecture/maven-as-is-inventory.md

printf "Audit artifacts are generated in %s/target/audit and docs/architecture\n" "${REPO_ROOT}"
