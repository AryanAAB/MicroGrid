# Net P2P Energy Trading Microgrid

This project is a net metering + peer-to-peer (P2P) energy trading simulation for a residential microgrid. It models how 
houses with different consumption and generation profiles interact with each other and with the main grid under various 
energy trading policies.

## What is Net Metering?
Net Metering is a billing mechanism that lets consumers who generate their own electricity (usually using solar panels) 
send excess electricity to the grid and get credited for it.

How it works:

- When solar panels produce more power than usage, the extra energy is exported to the grid.
- When consumption is higher than generation, energy is imported from the grid.
- The electricity meter keeps track of both import and export.
- At the end of the billing period, one pays for the net energy (energy imported - energy exported).

## What is P2P Energy Trading?
P2P energy trading allows electricity consumers and producers within a local area (microgrid) to trade energy directly 
with each other  instead of relying entirely on a central utility grid.

Participants may be:

- Consumers – only consume energy
- Producers – generate surplus energy (e.g., solar PV)
- Prosumers – both consume and generate energy

The grid acts as a backup, not the first option.

<figure>
  <img src="images/Consumer.png" alt="Sample meter reading of a consumer">
  <figcaption style="text-align: center;">Figure 1: Sample meter reading of a consumer</figcaption>
</figure>

<figure>
  <img src="images/Producer.png" alt="Sample meter reading of a producer">
  <figcaption style="text-align: center;">Figure 2: Sample meter reading of a producer</figcaption>
</figure>

<figure>
  <img src="images/Prosumer.png" alt="Sample meter reading of a prosumer">
  <figcaption style="text-align: center;">Figure 3: Sample meter reading of a prosumer</figcaption>
</figure>


## Modeling Net Metering with P2P Trading
To combine net metering and peer-to-peer energy trading in a single coherent framework, the microgrid is modeled as a set of interacting houses operating over discrete time intervals.

At each interval, every house locally accounts for its energy production, consumption, and historical grid interaction. Based on this accounting, houses may expose surplus energy for local sale or express a deficit to be satisfied by other participants.

The following section formally defines the system model, per-house energy accounting, and the market-clearing mechanism used to enable fair and efficient P2P energy exchange while preserving net-metering constraints.

## System Model
Consider a microgrid consisting of a set of houses:

$$\mathcal{H} = \{ h_1, h_2, \dots, h_N \}$$

Each house may simultaneously:
- consume electrical energy
- generate electrical energy (eg. rooftop solar panels)
- participate in local P2P trading
- exchange residual energy with the utility grid

Time is discretized into fixed-length intervals `t`.

## Per-House Energy Accounting (Energy Snapshot)
For each house `h` and interval `t`, an energy snapshot is computed using the following parameters:
- Interval production: $$P_h^t$$
- Interval consumption: $$C_h^t$$
- Selling price (ask): $$p_h^sell$$
- Cost price (bid): $$p_h^buy$$
- Sell threshold: $$θ_h$$
- Historical grid import: $$G_{h, import}^{hist}$$
- Historical grid export: $$G_{h, export}^{hist}$$

## Threshold-Based Grid Commitment
A fixed minimum quantity of generated energy is exported to the grid each time interval. This is to ensure that a user
can use the excess energy generated to compensate for grid imports in the future if needed.

$$G_{h,export}^{threshold} = min(θ_h,P_h^t)$$

The remaining production becomes eligible for local trading:

$$ P_h^{avail} = P_h^t - G_{h,export}^{threshold}$$

## Net Metering of Historical Consumption
Let the net historical grid deficit for a house `h` at time interval `t` be:

$$ D_h^{prev} = max(0, G_{h, import}^{hist} - G_{h, export}^{hist}) $$

The available production $$P_h^{avail}$$ first offsets this deficit. Any energy used for this purpose is still settled
through the grid to preserve net-metering consistency. 

## Current Interval Consumption Matching
