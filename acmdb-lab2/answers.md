---
title: Descriptions of Lab 1
author: Zhanghao Wu (516030910593)
---
# Descriptions of Lab 1
This is a required description file for lab 1 by **Zhanghao Wu** (516030910593)

## Design decisions
In this lab, the number of design decisions are quite small. They are listed as follows:
* **Handling the queries in Catalog**: In order to support quick queries of tables in the catalog by both *table ID* and *table name*, I created two maps one for mapping *table ID* to *table* and another for mapping *table name* to *table id*.
* **Iterator of HeapFile**: In order to iterate through the heap file without loading the whole file into the memory, I implemented the interface *DbFileIterator*. My implementation for the *DbFileIterator* contains iterator *tupleIterator* of a *HeapPage* and a *currentPid* as class members.  The iterator goes through the whole page that has *page id* of *currentPid*. Whenever the *tupleIterator* has no next element, the *currentPid* will be increased and the next page, if exists, will be loaded to the memory by the *BufferPool*. This makes sure that at most one page will be loaded to the memory, instead of the whole file.

## API changes
A class called Table is added to the Catalog, for convenience. The Table is just a collection of *table name*, *primary key field* and *database file*.

## Missing Element
I implemented all elements required in this lab.

## Timing and difficulties/confusing
It takes me about 6-7 hours to finish this project, including building the environment, reading the documents and implementing all the required part. As for difficulties or confusing, I actually spent some time finding out whether the *pageId* represents the ordinal of the page in a table, or is generated from UUID or other random numbers, since the *id* in the name of the variable is quite confusing (*pageId* represents the ordinal of a page in the table but *tableId* is generated from UUID in *Utility*).