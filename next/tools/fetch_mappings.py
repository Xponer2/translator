"""Downloads, for each version of the ladder, Mojang's official client mappings and Fabric's intermediary (the names
that stay the same across versions) into mappings/<version>/: client.txt and mappings.tiny. They stay on your machine
(git-ignored): the ladder's rename tables are generated from them.

usage: python tools/fetch_mappings.py [versions...]   (default: 1.21.1 1.21 1.20.6 1.20.4)
"""
import io
import json
import os
import sys
import urllib.request
import zipfile

MANIFEST = 'https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'
INTERMEDIARY = 'https://maven.fabricmc.net/net/fabricmc/intermediary/{v}/intermediary-{v}-v2.jar'
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'mappings')


def get(url):
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def main():
    versions = sys.argv[1:] or ['1.21.1', '1.21', '1.20.6', '1.20.4']
    manifest = json.loads(get(MANIFEST))
    for version in versions:
        folder = os.path.join(ROOT, version)
        os.makedirs(folder, exist_ok=True)
        entry = next(v for v in manifest['versions'] if v['id'] == version)
        info = json.loads(get(entry['url']))
        with open(os.path.join(folder, 'client.txt'), 'wb') as out:
            out.write(get(info['downloads']['client_mappings']['url']))
        with zipfile.ZipFile(io.BytesIO(get(INTERMEDIARY.format(v=version)))) as jar:
            with open(os.path.join(folder, 'mappings.tiny'), 'wb') as out:
                out.write(jar.read('mappings/mappings.tiny'))
        print(version, 'ok')


if __name__ == '__main__':
    main()
