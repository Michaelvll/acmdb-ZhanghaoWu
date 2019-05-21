---
title: Descriptions of Lab 3
author: Zhanghao Wu (516030910593)
---
# Descriptions of Lab 3
This is a required description file for lab 3 by **Zhanghao Wu** (516030910593)

## Design decisions
For now my modified LRU (least recently used) as my page eviction policy. This modified LRU will first try to evict the least recently used non-dirty page (i.e. no write back is needed) in the buffer. If all the pages are dirty, it will then evict the least recently used dirty page. For the *Join* operation, I apply *HashEquiJoin* as the backbone when the operator is *equals* and use nested-loop join for other operators.

## API changes
For convenience, I added a *merge* method for the *Tuple* class so that the different *Join* methods can use the method to generate the joined output.

## Missing Element
I implemented all elements required in this lab.

## Timing and difficulties/confusing
It takes me about 5-6 hours to finish this project, including reading the documents, implementing all the required parts and debugging. As for difficulties, I found that there are some bugs in my implementation of iteration for *HeapFile*. After supporting the deletion, the iterator should support to skip the empty pages, which I did not notice in the previous implementation. And for the three query examples in the document, it takes a lot of time to get the result if we use the pure nested-loop join as the backbone.