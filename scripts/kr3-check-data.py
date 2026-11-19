#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Compare only the original KR1 fields in the original and migrated databases."""
from pathlib import Path
import subprocess,json,hashlib
root=Path(__file__).resolve().parents[1]
columns={'applicants':'id,full_name,email,phone,status','examiners':'id,full_name,email,subject,status','exam_applications':'id,applicant_id,exam_name,scheduled_at,status,score,created_at'}
def read(container,user,database,table,fields):
 sql=f"select coalesce(json_agg(t order by id),'[]'::json) from (select {fields} from {table}) t"
 return json.loads(subprocess.check_output(['docker','exec',container,'psql','-U',user,'-d',database,'-Atc',sql],text=True))
out=[]
for table,fields in columns.items():
 original=read('exam-center-kr2-postgres-1','exam_center','exam_center',table,fields)
 migrated=read('exam-center-kr3-postgres-1','exam_web','exam_center_kr3',table,fields)
 a=hashlib.sha256(json.dumps(original,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
 b=hashlib.sha256(json.dumps(migrated,sort_keys=True,ensure_ascii=False).encode()).hexdigest()
 assert original==migrated,table+' original fields differ'
 out.append({'table':table,'rows':len(original),'originalSha256':a,'migratedSha256':b,'identical':True})
dest=root/'docs/kr3/qa/data-migration.json';dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text(json.dumps({'date':'2026-11-19','tables':out},indent=2),encoding='utf-8')
print('Original KR1 fields match the KR3 copy in all three tables: '+', '.join(str(x['rows']) for x in out))
