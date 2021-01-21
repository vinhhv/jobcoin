# Jobcoin Mixer
An example project on how to obfuscate transactions by allowing users to automatically distribute their deposits across
many owned addresses. The distribution is achieved by a mixing service which pulls from the users deposit address and
over time randomly distributes a percentage of the total deposit address' value across all the user's linked owned
addresses.

### Flow
1. The user provides a list of owned addresses
2. The mixer creates a new deposit address
3. The user deposits to that address
4. The mixer continually checks the deposit address for any balance above 0
5. If balance > 0, the mixer will transfer the coin to a distribution housing account
6. Over time, the mixer will randomly distribute a percentage of the total housing balance across the linked accounts in
step 1

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

## Details
The entire application is a Finch service using a Cats backend. More details on each component:

### Transfer Service
The core service which sends coins to the targeted addresses. If the target address is a deposit address, it will
add the transaction to a queue which will be picked up by a background process, which ultimately moves that coin
over to the housing account for later mixing.

### Deposit Transfer Service
This service owns an in-memory queue which is populated with transactions where the target address is a deposit address.
Over time, the service pulls from the queue and transfers the coin from the deposit address to its linked housing/mixing
address.

### Mixing Service
This service on a (configured, see `application.conf`) timed basis randomly selects a certain portion P of the housing
account's balance and randomly distributes it over the linked sink addresses created by the user. The algorithm
chooses N-1 (N being the number of sink addresses) random numbers between 0 and P, sorts the numbers, and grabs the
differences between those numbers. Those differences will act as the random distribution across all sink addresses.

This service is also responsible for linking the deposit, housing and sink addresses together using two in memory
maps whenever a client decides to create this mixing pipeline (`create_deposit_address.py`).

## Testing
I've written a few tests to exhibit what types of tests and what key areas need to be tested. Given more time, I would
have full coverage, but I believe the tests are sufficient examples to indicate those major areas. Other key areas I
would have tested are, for example, the deposit transfer service: testing if all the transactions to the deposit address
are atomically transferred to its linked housing address.