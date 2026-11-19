#!/usr/bin/env python3
"""Deploy the source ZIP in an isolated Compose project and compare tested source hashes."""
from pathlib import Path
import hashlib,json,os,secrets,socket,subprocess,tempfile,urllib.request
from zipfile import ZipFile
ROOT=Path(__file__).resolve().parents[1]
ARCHIVE=ROOT/'dist/exam-center-spring-source.zip'
def code_hash(root):
    paths=[]
    for top in ['common','desktop','backend','sql','scripts','docker']:
        paths += [p for p in (root/top).rglob('*') if p.is_file() and not any(s in {'target','__pycache__','.DS_Store'} for s in p.parts)]
    paths += [root/p for p in ['pom.xml','compose.yaml','.env.example','.gitignore','.dockerignore','run.sh','run.ps1','Запустить КР2.command','Демонстрация КР2.command']]
    h=hashlib.sha256()
    for p in sorted(paths):
        h.update(p.relative_to(root).as_posix().encode());h.update(b'\0');h.update(p.read_bytes());h.update(b'\0')
    return h.hexdigest()
with ZipFile(ARCHIVE) as z:
    names=z.namelist()
    assert not any(n.endswith('/.env') or '/config/' in n or '/target/' in n or '/student-hub-suite-rebuilt/' in n for n in names)
folder=Path(tempfile.mkdtemp(prefix='exam spring clean '))
subprocess.run(['unzip','-q',str(ARCHIVE),'-d',str(folder)],check=True)
project=folder/'exam-center-spring'
assert code_hash(ROOT)==code_hash(project),'Archive code differs from working project'
with socket.socket() as a,socket.socket() as b:
    a.bind(('127.0.0.1',0));b.bind(('127.0.0.1',0));dbport=a.getsockname()[1];apiport=b.getsockname()[1]
password=secrets.token_hex(24)
(project/'.env').write_text(f'EXAM_DB_NAME=exam_center_release\nEXAM_DB_USER=exam_center_release\nEXAM_DB_PASSWORD={password}\nEXAM_DB_PORT={dbport}\nEXAM_API_PORT={apiport}\n')
(project/'.env').chmod(0o600)
compose_name='exam-spring-release-'+secrets.token_hex(4)
env=os.environ.copy();env['COMPOSE_PROJECT_NAME']=compose_name;env.pop('EXAM_TEST_API_URL',None)
report={'date':'2026-10-23','clean_directory_with_spaces':True,'secrets_in_source_archive':False,'architecture':'JavaFX -> REST -> Spring Boot -> PostgreSQL','tested_code_sha256':code_hash(project),'source_archive_files':len(names)}
def run(args,name):
    with (folder/name).open('w') as out:subprocess.run(args,cwd=project,env=env,stdout=out,stderr=subprocess.STDOUT,check=True)
try:
    run(['./scripts/setup.sh'],'setup.log')
    def snapshot():
        with urllib.request.urlopen(f'http://127.0.0.1:{apiport}/api/snapshot') as f:return json.load(f)
    first=snapshot();counts={k:len(v) for k,v in first.items()};assert counts=={'applicants':5,'applications':10,'examiners':5},counts
    run(['./scripts/setup.sh'],'setup-repeat.log');assert snapshot()==first
    run(['./scripts/test.sh'],'tests.log')
    report.update({'fresh_database_counts':counts,'setup_idempotent':True,'unit_jdbc_http_tests':'PASS'})
    (ROOT/'docs/qa/release-check.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(report,ensure_ascii=False))
    print('Build logs:',folder)
finally:
    # Only this fresh Compose project and its newly created volume may be removed.
    run(['docker','compose','-p',compose_name,'down','--volumes'],'cleanup.log')
