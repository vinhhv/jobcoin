# Jobcoin Mixer
An example project on how to obfuscate transactions by allowing users to automatically distribute their deposits across many owned addresses.
The distribution is achieved by a mixing service which pulls from the users deposit address and over time randomly distributes a percentage of the total deposit address' value across all of the user's linked owned addresses.

1. The user provides a list of owned addresses
2. The mixer creates a new deposit address
3. The user deposits to that address
4. The mixer continually checks the deposit address for any balance above 0
5. If balance > 0, the mixer will transfer the coin to a distribution housing account
6. Over time, the mixer will randomly distribute a percentage of the total housing balance across the linked accounts in step 1

## High Level Components
