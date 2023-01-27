# ACID

```markdown
The safety guarantees provided by transactions are often described by the well-known acronym ACID, which stands for
Atomicity, Consistency, Isolation, and Durability. It was coined in 1983 by Theo Härder and Andreas Reuter [7] in an
effort to establish precise terminology for fault-tolerance mechanisms in databases.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 351). O'Reilly Media. Kindle Edition. 
```

### Atomicity

```markdown
If the writes are grouped together into an atomic transaction, and the transaction cannot be completed (committed) due
to a fault, then the transaction is aborted and the database must discard or undo any writes it has made so far in that
transaction.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 352). O'Reilly Media. Kindle Edition. 
```

### Consistency

```markdown
The idea of ACID consistency is that you have certain statements about your data (invariants) that must always be true—

Kleppmann, Martin. Designing Data-Intensive Applications (p. 353). O'Reilly Media. Kindle Edition.

this idea of consistency depends on the application’s notion of invariants, and it’s the application’s responsibility to
define its transactions correctly so that they preserve consistency. This is not something that the database can
guarantee: if you write bad data that violates your invariants, the database can’t stop you.

Kleppmann, Martin. Designing Data-Intensive Applications (pp. 353-354). O'Reilly Media. Kindle Edition. 
```

### Isolation

```markdown
Isolation in the sense of ACID means that concurrently executing transactions are isolated from each other: they cannot
step on each other’s toes.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 354). O'Reilly Media. Kindle Edition. 
```

### Durability

```markdown
Durability is the promise that once a transaction has committed successfully, any data it has written will not be
forgotten, even if there is a hardware fault or the database crashes.

Kleppmann, Martin. Designing Data-Intensive Applications (p. 355). O'Reilly Media. Kindle Edition. 
```
