---
title: Descriptions of Lab 4
author: Zhanghao Wu (516030910593)
---
# Descriptions of Lab 4
This is a required description file for lab 4 by **Zhanghao Wu** (516030910593)

## Design decisions
For lab 4, I implemented the histogram for selectivity estimation. As mentioned in the documents, whenever a request comes, the method *estimateSelectivity* estimate the selectivity by summing the heights of buckets that satisfies the operator and value in the histogram. For join ordering, I implemented the *orderJoins* method following the pseudocode.  

## API changes
I did no change to the APIs, apart from some utility methods for convenience.

## Missing Element
I implemented all elements required in this lab.

## Timing and difficulties/confusing
It took me about 4-5 hours to finish this project, including reading the documents, implementing all the required parts and debugging. As for difficulties, I think this task is not that hard to implement, and it is very useful having that pseudocode for *estimateSelectivity*.