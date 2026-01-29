# Net P2P Energy Trading Microgrid

This project is a net metering + peer-to-peer (P2P) energy trading simulation for a residential microgrid. It models how 
houses with different consumption and generation profiles interact with each other and with the main grid under the net metering + peer-to-peer trading policy.

## What is Net Metering?
Net Metering is a billing mechanism that lets consumers who generate their own electricity (usually using solar panels) 
send excess electricity to the grid and get credited for it.

How it works:

- When solar panels produce more electricity than usage, the extra energy is exported to the grid.
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

In case of any surplus or deficit, the main grid acts as a backup but is not the first option.

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

### Per-House Energy Accounting (Energy Snapshot)
For each house `h` and interval `t`, an energy snapshot is computed using the following parameters:
- Interval production: $$P_h^t$$
- Interval consumption: $$C_h^t$$
- Selling price (ask): $$p_h^{sell}$$
- Cost price (bid): $$p_h^{buy}$$
- Sell threshold: $$θ_h$$
- Historical grid import: $$G_{h, import}^{hist}$$
- Historical grid export: $$G_{h, export}^{hist}$$

The time interval is typically kept short, such as 15 minutes to 1 hour, to ensure fairness in energy allocation. Short 
intervals allow energy produced by a participant at a given time to be matched with consumption occurring simultaneously. 
Longer intervals could allow a producer’s energy to be allocated to consumers whose demands occur much later, which may 
disadvantage participants who are consuming at the same time the energy is produced.

### Threshold-Based Grid Commitment
A fixed minimum quantity of generated energy is exported to the grid each time interval. This is to ensure that a user
can use the excess energy generated to compensate for grid imports in the future if needed.

$$G_{h,export}^{threshold} = min(θ_h,P_h^t)$$

The remaining production becomes eligible for local trading:

$$ P_h^{thresh} = P_h^t - G_{h,export}^{threshold}$$

### Net Metering of Historical Consumption
Let the net historical grid deficit for a house `h` at time interval `t` be:

$$ D_h^{prev} = max(0, G_{h, import}^{hist} - G_{h, export}^{hist}) $$

The available production after threshold $$P_h^{thresh}$$ first offsets this deficit. Any energy used for this purpose is still settled
through the grid to preserve net-metering consistency. 

$$P_h^{avail} = max(0, P_h^{thresh} - D_h^{prev})$$

### Current Interval Consumption Matching
If remaining production satisfies consumption:

$$P_h^{avail} \geq C_h^t$$

then:
- consumption is fully met
- surplus is created

else:
- all production is absorbed by the house
- a deficit remains

Formally:

$$surplus_h = max(0, P_h^{avail} - C_h^t)$$
$$deficit_h = max(0, C_h^t - P_h^{avail})$$

### Snapshot Output
Finally, each house publishes

$$S^t_h = (surplus_h, deficit_h, p_h^{sell}, p_h^{buy})$$

These snapshots form the inputs to the P2P market.

## Market Participant Classification
Houses are partitioned into two disjoint sets:

- Sellers: $$\mathcal{S} = \{h | surplus_h > 0\}$$
- Buyers: $$\mathcal{B} = \{h | deficit_h > 0\}$$

## Market Ordering
To perform price-based clearing,

sellers are sorted by
- increasing selling price
- decreasing surplus

buyers are sorted by
- decreasing buying price
- increasing deficit

This ordering ensures that
- lowest-cost energy is supplied first
- highest-value demand is satisfied first

This is done so that energy is allocated to the highest-priced demand first because buyers with greater willingness to pay
derive higher marginal utility from energy consumption. Moreover, for a given price, highest-value demand is satisfied 
first because buyers with larger energy deficits experience greater utility from additional energy. Prioritizing such
demand ensures that limited local supply is allocated to those for whom the energy is most immediately useful.

## Window-Based Market Clearing
The algorithm proceeds by selecting price and deficit/surplus homogenous windows of buyers and sellers. That is, all
participants who are selling or buying at the same price and have the same deficit or surplus are treated equally by the 
algorithm. This ensures fairness for all participants.

This forms two windows
- $$\mathcal{B}$$ the buyer window in which all buyers have the same deficit and the same buying price
- $$\mathcal{S}$$ the seller window in which all sellers have the same surplus and the same selling price

Let:
- `b` be the buyer representative
- `s` be the seller representative

Market clearing proceeds only if

$$p_b^{buy} \geq p_s^{sell}$$

Otherwise, no mutually beneficial trade exists. 

## Aggregation
Within each window, let

$$ \mathcal{D} = \sum \text{deficit} $$
$$ \mathcal{S} = \sum \text{surplus} $$

The traded energy is

$$ \mathcal{E} = min(\mathcal{D}, \mathcal{S}) $$

The transaction price is the mid-market clearing price:

$$ p = \frac{p_b^{buy} + p_s^{sell}}{2} $$

## Allocation Rule
Energy is allocated uniformly within the active window (since each seller has the same surplus and each buyer has the same deficit in a window).

Each buyer receives

$$ \frac{E}{|B_{window}|} $$

Each seller supplies:

$$ \frac{E}{|S_{window}|} $$

This avoids intra-window discrimination and simplifies settlement.

## Iterative Clearing
After each clearing step,
- satisfied buyers are removed
- exhausted sellers are removed
- the process repeats

until,
- demand is zero
- supply is zero
- or price compatibility fails

The residual energy is settled with the grid.

## Economic Interpretation
The NetP2P energy trading in a residential microgrid provides several economic advantages over traditional net metering policy:
- **Reduced Energy Costs for Participants**
  - Buyers can purchase energy locally at prices typically lower than the grid purchase rates, while sellers can earn revenue by selling excess energy to neighbors instead of exporting at lower feed-in tariffs.
  - This reduces net electricity costs for the community and increases incentives for local generation (e.g., solar PV).
- **Optimal Local Utilization of Renewable Energy**
  - Because energy is produced and consumed locally, **local energy has lower technical losses such as transmission costs and peak charges**, so the effective cost per kWh is lower for buyers.
  - The **P2P price reflects this lower marginal cost** while still rewarding the sellers.
- **Encouragement of Renewable Investment**
  - Participants see a **direct economic return from generating surplus energy**, making investments in rooftop solar and other renewable energies more attractive.
  - This creates a **positive feedback loop**: more local generation → more local trading → more economic benefits.
- **Reduced Grid Dependency and Infrastructure Costs**
  - By satisfying local demand with local generation, the system **reduces peak load on the grid** over time.
  - **Less reliance on grid energy** can reduce costs for both utilities and consumers.

## Fairness in NetP2P Energy Trading
- **Self-Consumption and Net-Metering Fairness**
  - Each house is allowed to first satisfy its **own cumulative demand** using its generated energy through net metering before participating in P2P trading.
  - Each house also has an option to include a threshold value before selling to P2P trading to **allow current generation to satisfy future demand**.
  - This ensures participants are not disadvantaged for investing in local generation and are not forced to sell energy externally while still having unmet personal demand.
- **Intra-Window Fairness**
  - Houses grouped within the **same price–surplus or price–deficit window are allocated energy uniformly**, preventing arbitrary preference among participants with identical economic and energy characteristics.
- **Proportional Allocation**
  - The total tradable energy in a clearing window is distributed proportionally among participating houses, ensuring that **no single participant captures a disproportionate share of the trade**.
- **Time-Interval Fairness**
  - Short time intervals (e.g., 15 minutes to 1 hour) ensure that **energy produced at a given time is matched with contemporaneous consumption**, preventing producers from supplying future demand at the expense of participants consuming simultaneously.

## Simulation
