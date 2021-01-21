#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import requests
import sys


if len(sys.argv) != 4:
    print('ERROR: Please only provide from address, to address, and amount in that order')
    sys.exit(1)

from_address = sys.argv[1]
to_address = sys.argv[2]
amount = sys.argv[3]

data = {
    'fromAddress': from_address,
    'toAddress': to_address,
    'amount': amount
}

response = requests.post('http://localhost:8081/sendCoins', json=data)

print(response.json())
