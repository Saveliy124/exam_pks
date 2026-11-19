#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Start a disposable Docker backend on a *_test database, run browser tests, clean up."""
from pathlib import Path
import subprocess,os,sys,base64,secrets,json,time,urllib.request,shutil
root=Path(__file__).resolve().parents[1]
values={}
for line in (root/'.env').read_text(encoding='utf-8').splitlines():
 if line and not line.startswith('#') and '=' in line:
  k,v=line.split('=',1);values[k]=v
compose=['docker','compose','--env-file',str(root/'.env'),'-f',str(root/'web-backend/docker-compose.yml')]
db='exam_center_browser_test';assert db.endswith('_test')
user=values.get('EXAM_WEB_DB_USER','exam_web')
subprocess.run(compose+['up','-d','--wait','postgres'],check=True,cwd=root)
for cmd in [['dropdb','--if-exists','-U',user,db],['createdb','-U',user,db]]:subprocess.run(compose+['exec','-T','postgres']+cmd,check=True,cwd=root)
testenv=os.environ.copy()
testenv.update(SPRING_DATASOURCE_URL='jdbc:postgresql://postgres:5432/'+db,SPRING_DATASOURCE_USERNAME=user,SPRING_DATASOURCE_PASSWORD=values['EXAM_WEB_DB_PASSWORD'],JWT_SECRET_BASE64=base64.b64encode(secrets.token_bytes(48)).decode(),BOOTSTRAP_ADMIN_EMAIL='admin@example.org',BOOTSTRAP_ADMIN_PASSWORD='Start1!BrowserSession',CORS_ALLOWED_ORIGINS='http://127.0.0.1:8080,http://localhost:8080',PERMANENT_DELETE_ENABLED='true')
name='exam-center-browser-'+secrets.token_hex(4)
cmd=['docker','run','-d','--name',name,'--label','examcenter.test=true','--network','exam-center-kr3_default','-p','127.0.0.1::8080']
for key in ['SPRING_DATASOURCE_URL','SPRING_DATASOURCE_USERNAME','SPRING_DATASOURCE_PASSWORD','JWT_SECRET_BASE64','BOOTSTRAP_ADMIN_EMAIL','BOOTSTRAP_ADMIN_PASSWORD','CORS_ALLOWED_ORIGINS','PERMANENT_DELETE_ENABLED']:cmd+=['-e',key]
qa=root/'docs/kr3/qa';qa.mkdir(parents=True,exist_ok=True)
try:
 subprocess.run(cmd+['exam-center-web:3.0.0'],env=testenv,check=True,stdout=subprocess.DEVNULL)
 port=json.loads(subprocess.check_output(['docker','inspect',name]))[0]['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
 url='http://127.0.0.1:'+port
 # Same-origin browser requests carry the actual randomly allocated origin.
 # The security configuration permits only configured origins. Restart with the known mapped port.
 subprocess.run(['docker','stop',name],check=True,stdout=subprocess.DEVNULL)
 subprocess.run(['docker','rm',name],check=True,stdout=subprocess.DEVNULL)
 testenv['CORS_ALLOWED_ORIGINS']=url
 cmd[cmd.index('127.0.0.1::8080')]='127.0.0.1:'+port+':8080'
 subprocess.run(cmd+['exam-center-web:3.0.0'],env=testenv,check=True,stdout=subprocess.DEVNULL)
 for _ in range(60):
  try:
   with urllib.request.urlopen(url+'/actuator/health',timeout=2) as r:
    if r.status==200:break
  except Exception:time.sleep(1)
 else:raise RuntimeError('Test backend did not become healthy')
 node=os.environ.get('KR3_NODE') or shutil.which('node') or str(Path.home()/'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node')
 testenv.update(KR3_TEST_WEB_URL=url,KR3_ALLOW_TEST_MUTATIONS='1')
 result=subprocess.run([node,str(root/'scripts/kr3-browser-test.mjs')],cwd=root,env=testenv)
 sys.exit(result.returncode)
finally:
 logs=subprocess.run(['docker','logs',name],capture_output=True,text=True)
 private=qa/'private';private.mkdir(exist_ok=True);(private/'browser-backend.log').write_text(logs.stdout+logs.stderr,encoding='utf-8')
 subprocess.run(['docker','rm','-f',name],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
