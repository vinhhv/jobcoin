#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import requests
import sys


if len(sys.argv) < 2:
    print('ERROR: Please provide at least one address')
    sys.exit(1)

addresses = sys.argv[1:]

data = {
    'addresses': addresses
}

response = requests.post('http://localhost:8081/createMixer', json=data)

print(response.json())
