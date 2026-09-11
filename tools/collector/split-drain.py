#!/usr/bin/env python3
"""把收集站撈回來的那一包拆成兩份。

用法：

    python tools/collector/split-drain.py raw.json inbox.json ack.json

``raw.json`` 是 ``/drain`` 的回應，長得跟 ``captured.json`` 一樣，只是多一個
``_keys``——佇列裡那些條目的鍵。

拆成兩份的理由是「撈的時候不刪」：``inbox.json`` 拿去分類，``ack.json`` 留到
PR 真的開好了才送回 ``/ack`` 去刪。中間哪一步掛掉，下一次還撈得到同一批，
不會平白掉資料。

印出條目數，工作流程拿它決定要不要繼續。
"""

import json
import sys
from pathlib import Path


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print(__doc__)
        return 2
    raw, inbox, ack = (Path(a) for a in argv)
    data = json.loads(raw.read_text(encoding="utf-8"))
    keys = data.pop("_keys", [])
    inbox.write_text(json.dumps(data, ensure_ascii=False, indent=1),
                     encoding="utf-8")
    ack.write_text(json.dumps({"keys": keys}), encoding="utf-8")
    print(len(data.get("entries", {})))
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
