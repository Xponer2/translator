"""
Runs GenFlattening.java: writes src/main/resources/data/translator/flattening.json from Minecraft 1.21.1's own
DataFixer (see the class comment there), keeping the entries already in the file where they differ, and a report.

    python -I tools/gen_flattening/run.py --legacy-jar <deobfuscated 1.12.2 jar> [--report <md>]
        [--client-jar <minecraft 1.21.1 client jar>] [--mappings <its client mappings txt>] [--manifest <version json>]
        [--libraries <a launcher's libraries dir>] [--java <java 21+>]

Defaults: the client jar, mappings and version json in ~/.gradle/caches/neoformruntime/artifacts (any NeoForge 1.21.1
workspace has them); the libraries of the version json from the Gradle cache (or --libraries); ASM from the Gradle cache.
"""
import argparse
import glob
import json
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
GRADLE = os.path.join(os.path.expanduser('~'), '.gradle', 'caches')
ARTIFACTS = os.path.join(GRADLE, 'neoformruntime', 'artifacts')


def library(name, libraries):
    group, artifact, version = name.split(':')[:3]
    file = f'{artifact}-{version}.jar'
    if libraries:
        path = os.path.join(libraries, *group.split('.'), artifact, version, file)
        if os.path.isfile(path):
            return path
    hits = glob.glob(os.path.join(GRADLE, 'modules-2', 'files-2.1', group, artifact, version, '*', file))
    return hits[0] if hits else None


def newest(group, artifact):
    def key(path):
        return [int(p) if p.isdigit() else 0 for p in os.path.basename(os.path.dirname(os.path.dirname(path))).split('.')]
    hits = [h for h in glob.glob(os.path.join(GRADLE, 'modules-2', 'files-2.1', group, artifact, '*', '*', f'{artifact}-*.jar'))
            if not h.endswith(('-sources.jar', '-javadoc.jar'))]
    return max(hits, key=key) if hits else None


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--legacy-jar', required=True)
    p.add_argument('--client-jar', default=os.path.join(ARTIFACTS, 'minecraft_1.21.1_client.jar'))
    p.add_argument('--mappings', default=os.path.join(ARTIFACTS, 'minecraft_1.21.1_client_mappings.txt'))
    p.add_argument('--manifest', default=os.path.join(ARTIFACTS, 'minecraft_1.21.1_version_manifest.json'))
    p.add_argument('--libraries')
    p.add_argument('--asm', nargs=2, metavar=('ASM', 'ASM_TREE'))
    p.add_argument('--out', default=os.path.join(ROOT, 'src', 'main', 'resources', 'data', 'translator', 'flattening.json'))
    p.add_argument('--report', default=os.path.join(ROOT, 'build', 'flattening-report.md'))
    p.add_argument('--java', default=os.path.join(os.environ['JAVA_HOME'], 'bin', 'java') if 'JAVA_HOME' in os.environ else 'java')
    a = p.parse_args()

    with open(a.manifest, encoding='utf-8') as f:
        manifest = json.load(f)
    classpath = [a.client_jar]
    missing = []
    for lib in manifest['libraries']:
        if len(lib['name'].split(':')) > 3:
            continue  # natives
        path = library(lib['name'], a.libraries)
        (classpath.append(path) if path else missing.append(lib['name']))
    if missing:
        sys.exit('libraries not found (pass --libraries <dir>): ' + ', '.join(missing))
    asm = a.asm or [newest('org.ow2.asm', 'asm'), newest('org.ow2.asm', 'asm-tree')]
    if None in asm:
        sys.exit('ASM not found in the Gradle cache: pass --asm <asm.jar> <asm-tree.jar>')
    classpath += asm
    command = [a.java, '-Xmx2g', '-cp', os.pathsep.join(classpath), os.path.join(HERE, 'GenFlattening.java'),
               '--client-jar', a.client_jar, '--mappings', a.mappings, '--legacy-jar', a.legacy_jar,
               '--out', a.out, '--report', a.report]
    sys.exit(subprocess.call(command))


if __name__ == '__main__':
    main()
