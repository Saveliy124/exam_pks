#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Recreate ONLY the dedicated *_test database and run the full Spring/PostgreSQL suite."""
from pathlib import Path
import os, subprocess, sys
root=Path(__file__).resolve().parents[1]
env={}
for line in (root/'.env').read_text(encoding='utf-8').splitlines():
    if line and not line.startswith('#') and '=' in line:
        k,v=line.split('=',1);env[k]=v
name='exam_center_kr3_test'
assert name.endswith('_test')
compose=['docker','compose','--env-file',str(root/'.env'),'-f',str(root/'web-backend/docker-compose.yml')]
subprocess.run(compose+['up','-d','--wait','postgres'],check=True,cwd=root)
user=env.get('EXAM_WEB_DB_USER','exam_web')
for cmd in [['dropdb','--if-exists','-U',user,name],['createdb','-U',user,name]]:
    subprocess.run(compose+['exec','-T','postgres']+cmd,check=True,cwd=root)
runenv=os.environ.copy()
runenv.update(KR3_TEST_DB_URL='jdbc:postgresql://127.0.0.1:'+env.get('EXAM_WEB_DB_PORT','55435')+'/'+name,
              KR3_TEST_DB_USER=user,KR3_TEST_DB_PASSWORD=env['EXAM_WEB_DB_PASSWORD'])
cmd=['mvn']
if os.environ.get('KR3_MAVEN_SETTINGS'):cmd+=['-s',os.environ['KR3_MAVEN_SETTINGS']]
cmd+=['-pl','web-backend','-am','verify','-Pkr3-integration']
sys.exit(subprocess.run(cmd,cwd=root,env=runenv).returncode)
