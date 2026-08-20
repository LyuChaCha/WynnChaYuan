#!/usr/bin/env bash
# 從 Modrinth 抓對應本專案 Minecraft 版本的 Wynntils fabric jar 放進 libs/。
#
# 為什麼需要這支
# --------------
# Wynntils 沒有發佈到任何 Maven repo，只能拿 jar 檔編譯（見 build.gradle 的說明）。
# 那份 jar 是別人的作品，不進本專案的版控，所以 CI 每次都得自己抓。
#
# 版本從 gradle.properties 讀，不寫死 —— 否則升 Minecraft 版本時這裡會默默
# 抓到舊的，編出來的東西對不上而且不會有人發現。
set -euo pipefail

cd "$(dirname "$0")/.."
MC_VERSION=$(grep '^minecraft_version=' gradle.properties | cut -d= -f2)
mkdir -p libs

if compgen -G "libs/wynntils-*-fabric*.jar" > /dev/null; then
    echo "libs/ 已經有 Wynntils，略過下載"
    exit 0
fi

echo "查詢 Wynntils（Minecraft ${MC_VERSION}，fabric）…"
API="https://api.modrinth.com/v2/project/wynntils/version"
QUERY="?loaders=%5B%22fabric%22%5D&game_versions=%5B%22${MC_VERSION}%22%5D"

URL=$(curl -fsSL -H 'User-Agent: LyuChaCha/WynnChaYuan (build)' "${API}${QUERY}" \
      | python3 -c "
import json, sys
versions = json.load(sys.stdin)
if not versions:
    sys.exit('找不到對應 Minecraft ${MC_VERSION} 的 fabric 版本')
# 回傳已依發佈時間排序，取最新那個的主檔案
for f in versions[0]['files']:
    if f.get('primary'):
        print(f['url']); break
else:
    print(versions[0]['files'][0]['url'])
")

# 檔名要還原百分號編碼：Modrinth 的網址把 + 編成 %2B，直接拿來當檔名的話
# 版本號會變成 wynntils-4.2.8-fabric%2BMC-... 這種看不懂的東西
NAME=$(basename "${URL}" | python3 -c 'import sys,urllib.parse; print(urllib.parse.unquote(sys.stdin.read().strip()))')
echo "下載 ${NAME}"
curl -fsSL -o "libs/${NAME}" "${URL}"
ls -1 libs/
