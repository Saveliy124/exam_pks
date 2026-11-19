#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Append missing local secrets; never overwrite existing credentials or print secrets."""
from pathlib import Path
import secrets,base64,os
root=Path(__file__).resolve().parents[1];env=root/'.env'
text=env.read_text(encoding='utf-8') if env.exists() else ''
known={line.split('=',1)[0] for line in text.splitlines() if '=' in line and not line.startswith('#')}
values={'EXAM_WEB_DB_NAME':'exam_center_kr3','EXAM_WEB_DB_USER':'exam_web','EXAM_WEB_DB_PASSWORD':secrets.token_urlsafe(32),'EXAM_WEB_DB_PORT':'55435','EXAM_WEB_PORT':'8080','JWT_SECRET_BASE64':base64.b64encode(secrets.token_bytes(48)).decode(),'BOOTSTRAP_ADMIN_EMAIL':'admin@example.org','BOOTSTRAP_ADMIN_NAME':'Administrator','BOOTSTRAP_ADMIN_PASSWORD':'Start1!'+secrets.token_hex(14),'CORS_ALLOWED_ORIGINS':'http://localhost:8080,http://127.0.0.1:8080','COOKIE_SECURE':'false','PERMANENT_DELETE_ENABLED':'true'}
missing={k:v for k,v in values.items() if k not in known}
if missing:
 with env.open('a',encoding='utf-8') as f:f.write('\n# KR3 local settings (do not commit)\n'+''.join(k+'='+v+'\n' for k,v in missing.items()))
os.chmod(env,0o600)
print('Local KR3 settings are ready. Bootstrap login and temporary password: root .env file.')
