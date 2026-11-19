#!/usr/bin/env python3
"""Создаёт переносимый архив исходников без паролей, БД, кэшей и чужого проекта."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
target = root / 'dist' / 'exam-center-spring-source.zip'
target.parent.mkdir(exist_ok=True)
folders = ('common', 'desktop', 'backend', 'sql', 'scripts', 'docker', 'docs')
names = ('pom.xml', '.dockerignore', 'compose.yaml', '.env.example', '.gitignore', 'README.md', 'run.sh', 'run.ps1', 'Запустить КР2.command', 'Демонстрация КР2.command')
files = [root / name for name in names]
files += [file for folder in folders for file in (root / folder).rglob('*') if file.is_file() and not any(p in {'__pycache__', '.DS_Store', 'target'} for p in file.parts)]
with ZipFile(target, 'w', ZIP_DEFLATED) as archive:
    for file in sorted(files):
        archive.write(file, Path('exam-center-spring') / file.relative_to(root))
import shutil
shutil.copyfile(target, root / 'dist' / 'exam-center-kr2-source.zip')
print(target)
