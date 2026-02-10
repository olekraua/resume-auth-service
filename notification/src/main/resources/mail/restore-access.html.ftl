<!doctype html>
<html>
  <body>
    <p>Hello<#if firstName?has_content> ${firstName?html}</#if>,</p>
    <p>To reset your password, open this link:</p>
    <p><a href="${link?html}">${link?html}</a></p>
    <p>This link expires in ${tokenTtl?html}.</p>
    <p>If you did not request a reset, you can ignore this email.</p>
  </body>
</html>
