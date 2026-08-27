"""把字型的字符表抽成碼位區間，讓 mod 在執行期知道哪些字畫得出來。

為什麼需要這一份：對話的就地取代得先確定譯文<b>每一個字</b>都畫得出來，
不然畫面上會出現一排方框——那比留著英文原文糟糕得多。字型的覆蓋率查不出來
就只能等玩家回報，而回報時看到的已經是壞掉的畫面了。

用法：python tools/font-coverage.py <字型.ttf> <輸出.txt>
"""
import struct
import sys


def codepoints(path):
    data = open(path, 'rb').read()
    count = struct.unpack('>H', data[4:6])[0]
    tables = {}
    for i in range(count):
        at = 12 + i * 16
        tables[data[at:at + 4].decode('latin1')] = struct.unpack(
            '>II', data[at + 8:at + 16])
    start, _ = tables['cmap']
    subtables = struct.unpack('>H', data[start + 2:start + 4])[0]
    chosen = None
    for i in range(subtables):
        at = start + 4 + i * 8
        _, _, off = struct.unpack('>HHI', data[at:at + 8])
        fmt = struct.unpack('>H', data[start + off:start + off + 2])[0]
        # 4 是 BMP、12 是完整範圍；有 12 就用 12
        if fmt in (4, 12) and (chosen is None or fmt == 12):
            chosen = (fmt, start + off)
    fmt, at = chosen
    out = set()
    if fmt == 4:
        seg2 = struct.unpack('>H', data[at + 6:at + 8])[0]
        seg = seg2 // 2
        ends = [struct.unpack('>H', data[at + 14 + i * 2:at + 16 + i * 2])[0]
                for i in range(seg)]
        base = at + 16 + seg2
        starts = [struct.unpack('>H', data[base + i * 2:base + 2 + i * 2])[0]
                  for i in range(seg)]
        for s, e in zip(starts, ends):
            if e != 0xFFFF:
                out.update(range(s, e + 1))
    else:
        groups = struct.unpack('>I', data[at + 12:at + 16])[0]
        for i in range(groups):
            g = at + 16 + i * 12
            s, e, _ = struct.unpack('>III', data[g:g + 12])
            out.update(range(s, e + 1))
    return out


def ranges(points):
    out = []
    for cp in sorted(points):
        if out and cp == out[-1][1] + 1:
            out[-1][1] = cp
        else:
            out.append([cp, cp])
    return out


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        raise SystemExit(2)
    found = ranges(codepoints(sys.argv[1]))
    with open(sys.argv[2], 'w', encoding='utf-8', newline='\n') as f:
        f.write('# 由 tools/font-coverage.py 產生，不要手改\n')
        f.write('# 每一行是一段收錄的碼位（十六進位，含頭含尾）\n')
        for lo, hi in found:
            f.write('%X-%X\n' % (lo, hi))
    print('%s：%d 段、%d 個碼位' % (
        sys.argv[2], len(found), sum(h - l + 1 for l, h in found)))


if __name__ == '__main__':
    main()
