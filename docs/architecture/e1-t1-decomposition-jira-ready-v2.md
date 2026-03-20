# E1-T1 Decomposition (Jira-ready, v2)

ID: `E1-T1`  
Репозиторії: `resume-backend-infra`, `resume-auth-service`  
Ціль: ADR + цільова Maven-модель (`platform-parent` + `platform-bom`)  
Оцінка: `3 SP`

## Subtasks

| Subtask ID | Repo | Що робимо | Acceptance Criteria | SP |
|---|---|---|---|---:|
| `E1-T1-S1` | `resume-auth-service` | AS-IS аудит Maven-ланцюжка (parent, effective-pom, ключові transitive) | Є інвентар поточного стану; явно зафіксовано, які runtime deps зараз приходять через parent | 0.5 |
| `E1-T1-S2` | `resume-backend-infra` | Формалізація design drivers і архітектурних обмежень | Є список правил: parent = policy only, bom = versions only, сервіси = explicit deps only | 0.25 |
| `E1-T1-S3` | `resume-backend-infra` | Підготовка варіантів моделі (мінімум 2) + decision matrix | Є порівняння варіантів за критеріями: ізоляція, міграційна складність, керованість, ризики | 0.5 |
| `E1-T1-S4` | `resume-backend-infra` | Вибір цільового варіанту і naming/coordinates policy | Затверджені groupId/artifactId для platform-parent і platform-bom, правила версіонування зафіксовані | 0.25 |
| `E1-T1-S5` | `resume-backend-infra` | Діаграма inheritance/import (Mermaid/PlantUML) | Діаграма показує: service POM наслідує platform-parent, імпортує platform-bom, без runtime deps у parent | 0.25 |
| `E1-T1-S6` | `resume-backend-infra` | Написання ADR (Context, Decision, Consequences, Rollout, Rollback) | ADR повний, без відкритих критичних TODO; містить межі для E1-T2/E1-T5 | 0.75 |
| `E1-T1-S7` | `resume-backend-infra`, `resume-auth-service` | Архітектурний review і formal acceptance | Є явний статус Accepted + погоджені ownership/next steps | 0.25 |
| `E1-T1-S8` | `resume-backend-infra` | Handoff-пакет у backlog для імплементації | Є деталізовані input для E1-T2/E1-T3/E1-T4/E1-T5 (що саме змінювати, в якій послідовності) | 0.25 |

Разом: `3.0 SP`

## Артефакти, які мають з’явитися

1. ADR з рішенням по моделі parent + bom.
2. Діаграма inheritance/import.
3. AS-IS inventory для `resume-auth-service`.
4. Handoff до наступних задач `E1-T2..E1-T5`.

## Acceptance Criteria для E1-T1 (story-level)

1. ADR має статус `Accepted`.
2. Є валідна inheritance/import діаграма.
3. В ADR явно зафіксовано: `platform-parent` не містить runtime `<dependencies>`.
4. В ADR явно зафіксовано: версії керуються через `platform-bom` (`dependencyManagement`).
5. Є погоджений rollout-план на `E1-T2/E1-T5` без архітектурної неоднозначності.

## Definition of Done (E1-T1)

1. Усі артефакти закомічені та доступні команді.
2. Архітектурне рішення підтверджене відповідальними.
3. Наступні задачі (`E1-T2..E1-T5`) можуть стартувати без додаткового discovery.

## Related detailed documents

1. `resume-backend-infra/architecture/messenger-modernization-decomposition-v2.md`
2. `resume-backend-infra/architecture/maven-implementation-handoff-package-decomposition.md`
3. `resume-backend-infra/architecture/adr/ADR-0001-maven-parent-bom-governance.md`
4. `docs/architecture/maven-as-is-inventory.md`
5. `docs/architecture/maven-runtime-deps-from-parent.md`
