---
title: Descriptions of Lab 5
author: Zhanghao Wu (516030910593)
---
# Descriptions of Lab 5
This is a required description file for lab 5 by **Zhanghao Wu** (516030910593)

## Design decisions
For this lab, I made quite much design decisions. The locks are applied at page level.
Firstly, I create a *PageLock* class which contains both shared and exclusive lock for a page. Each page owns a *PageLock*, implying which transaction holds the lock for the page. A *lockManager* maps the *PageId* to the corresponding *PageLock*. 
For deadlock detection, I applied a dependency graph based lock manager. The dependency graph is implemented in the *DependencyGraph*. Whenever a transaction requires a lock for a page, a directed edge from the transaction to all transactions holding the lock will be added to the dependency graph. Also, the dependency graph will check if any cycle start from the requester transaction.
Finally, The **NO STEAL/FORCE** policy is applied for guarantee the ACID properties of the database.

## API changes
I did no change to the APIs, apart from some utility class and variables for convenience, including *PageLock*, *DependencyGraph* and other helper variables. With these classes and variables, transactions will share locks for reading or own the exclusive lock for writing, which keeps the database thread safe.

## Missing Element
I implemented all elements required in this lab and pass all the test.

## Timing and difficulties/confusing
It took me about 8-9 hours to finish this project, including reading the documents, implementing all the required parts and debugging. As for difficulties, I found that it is quite difficult to debug for the *BTreeTest* since the multi-thread program will perform differently each time we run it. It takes much time finding out that in my implementation, the pages are actually marked dirty after one action is done in the transaction. However, whenever there is a dead lock detected, an exception will be thrown and the dirty information will be lost. Therefore, I could not simply check whether a page should be discard by the dirty information, which caused a bug when testing.