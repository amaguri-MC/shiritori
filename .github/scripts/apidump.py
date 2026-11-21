import os, sys, zipfile, subprocess, json, glob, collections
out = sys.argv[1]; os.makedirs(out, exist_ok=True)
home = os.path.expanduser('~')
jars = glob.glob(home + '/.gradle/caches/**/*.jar', recursive=True) + glob.glob('.gradle/**/*.jar', recursive=True)
print('jars', len(jars))
def names(j):
    try: return zipfile.ZipFile(j).namelist()
    except Exception: return []
mc = [j for j in jars if 'net/minecraft/world/item/Item.class' in set(names(j))]
mc.sort(key=os.path.getsize, reverse=True)
print('mc jars', mc)
fab = [j for j in jars if 'net.fabricmc.fabric-api' in j or 'fabric-api' in os.path.basename(j)]
def dump(jar, prefix, dest):
    by = collections.defaultdict(list)
    for n in names(jar):
        if n.endswith('.class') and n.startswith(prefix):
            by[os.path.dirname(n)].append(n[:-6].replace('/', '.'))
    for pkg, cls in by.items():
        p = os.path.join(out, dest, pkg + '.txt'); os.makedirs(os.path.dirname(p), exist_ok=True)
        with open(p, 'a') as f:
            for i in range(0, len(cls), 300):
                r = subprocess.run(['javap', '-p', '-cp', jar] + cls[i:i+300], capture_output=True, text=True)
                f.write(r.stdout)
pick = [j for j in mc if os.path.basename(j) in ('minecraft-client.jar', 'minecraft-common.jar')] or mc[:1]
print('dumping', pick)
for j in pick:
    dest = 'minecraft-client' if 'client' in os.path.basename(j) else 'minecraft'
    dump(j, 'net/minecraft/', dest)
    dump(j, 'com/mojang/', dest)
    z = zipfile.ZipFile(j)
    for n in z.namelist():
        if n.endswith('/'): continue
        if n.startswith('data/minecraft/recipe/') or n.startswith('assets/minecraft/lang/'):
            p = os.path.join(out, 'files', n); os.makedirs(os.path.dirname(p), exist_ok=True)
            open(p, 'wb').write(z.read(n))
seen = set()
for j in fab:
    for n in names(j):
        if n.startswith('net/fabricmc/fabric/api/') and n.endswith('.class'):
            seen.add(j); break
for j in seen: dump(j, 'net/fabricmc/fabric/api/', 'fabric')
# ja_jp from asset index
for idx in glob.glob(home + '/.gradle/caches/**/indexes/*.json', recursive=True):
    try: d = json.load(open(idx))
    except Exception: continue
    o = d.get('objects', {}).get('minecraft/lang/ja_jp.json')
    if not o: continue
    h = o['hash']
    for cand in glob.glob(home + '/.gradle/caches/**/objects/' + h[:2] + '/' + h, recursive=True):
        p = os.path.join(out, 'files/assets/minecraft/lang/ja_jp.json'); os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, 'wb').write(open(cand, 'rb').read()); print('ja_jp from', cand); break
    break
if not os.path.exists(os.path.join(out, 'files/assets/minecraft/lang/ja_jp.json')):
    print('WARN ja_jp not found')
