#!/usr/bin/env python3
"""Summarize connected Android test results (JUnit XML)."""
import glob, re
for f in glob.glob('app/build/outputs/androidTest-results/connected/debug/*.xml'):
    s = open(f).read()
    print(re.search(r'<testsuites[^>]*>', s).group(0)[:90])
    for m in re.finditer(r'<testcase name="([^"]+)"[^>]*?(/>|>(.*?)</testcase>)', s, re.S):
        body = m.group(3) or ''
        if 'AssumptionViolatedException' in body:
            print('skip', m.group(1), '(not applicable to this screen size)')
            continue
        print('FAIL' if '<failure' in body else 'ok  ', m.group(1))
        if '<failure' in body:
            print('   ', body.strip()[:600])
