#!/usr/bin/env python
# -*- coding: utf-8 -*-

import requests
import sys


if len(sys.argv) != 2:
    print('ERROR: Please only provide a single address')
    sys.exit(1)

address = sys.argv[1]

response = requests.get('http://localhost:8081/balance/' + address)

print(response.json())
