# Jobcoin Mixer
An example project on how to obfuscate transactions by allowing users to automatically distribute their deposits across many owned addresses.
The distribution is achieved by a mixing service which pulls from the users deposit address and over time randomly distributes a percentage of the total deposit address' value across all of the user's linked owned addresses.

1. The user provides a list of owned addresses
2. The mixer creates a new deposit address
3. The user deposits to that address
4. The mixer continually checks the deposit address for any balance above 0
5. If balance > 0, the mixer will transfer the coin to a distribution housing account
6. Over time, the mixer will randomly distribute a percentage of the total housing balance across the linked accounts in step 1

# Getting Started

## Starting the server
Run `sbt compile run`

## Hitting the server
Here we are using python3 scripts to hit the server.

Install requirements `pip3 install -r requirements.txt`

### Getting Balance
`python3 get_balance.py ADDRESS`

### Creating Deposit Address
`python3 create_deposit_address.py ADDRESS1 ADDRESS2 ... ADDRESSN`

### Sending Coin
`python3 send_coins.py FROM_ADDRESS TO_ADDRESS AMOUNT`

## High Level Components
![alt text](https://github.com/vinhhv/jobcoin/blob/master/jobcoin.png)