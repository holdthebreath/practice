# mysql

## B树和B+树

**空间局部性原理：如果一个存储器的某个位置被访问，则它附近的位置也会被访问。**

### 1. B-树

B-树,这里的 B 表示 balance( 平衡的意思)，B-树是一种**多路自平衡的搜索树**，类似普通的平衡二叉树，但允许每个节点有更多的子节点。**B-树专门为外部存储器设计，如磁盘，它对于读取和写入大块数据有良好的性能，所以一般被用在文件系统及数据库中。**

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210509233559.jpeg)

B-树有如下特点:

1. 所有键值分布在整颗树中（索引值和具体data都在每个节点里）；
2. 任何一个关键字出现且只出现在一个结点中；
3. 搜索有可能在非叶子结点结束（最好情况O(1)就能找到数据）；
4. 在关键字全集内做一次查找,性能逼近二分查找；

#### B树的查找

假设每个节点有 n 个 key值，被分割为 n+1 个区间，B-树的 key 和 data 是聚合在一起的。**一般而言，根节点都在内存中，B-树以每个节点为一次磁盘 IO**。比如，若搜索 key 为 25 节点的 data，首先在根节点进行二分查找（因为 keys 有序，二分最快），判断 key 25 小于 key 50，所以定位到最左侧的节点，此时进行一次磁盘 IO，将该节点从磁盘读入内存，接着继续进行上述过程，直到找到该 key 为止。

![img](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510153818.png)

### 2. B+树

B+树是B-树的变体，也是一种**多路搜索树**, 它与 B- 树的不同之处在于:

1. 所有关键字存储在叶子节点出现,内部节点(非叶子节点并不存储真正的 data)
2. 为所有叶子结点增加了一个链指针

![img](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510154147.jpeg)

**因为内节点并不存储 data，所以一般B+树的叶节点和内节点大小不同，而B-树的每个节点大小一般是相同的，为一页。**

为了增加 **区间访问性**，一般会对B+树做一些优化。

![img](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510154726.png)

### 区别

1. **B-树查询时间复杂度不固定，与 key 在树中的位置有关，最好为O(1)。**（查找数据在第一层）

2. **B+树所有的 data 域都在根节点，所以查询 key 的节点必须从根节点索引到叶节点，时间复杂度固定为 O(log n)。**

3. **B+树叶节点两两相连可大大增加区间访问性，可使用在范围查询等，而B-树每个节点 key 和 data 在一起，无法区间查找**

   ![img](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510154937.png)

   B+树利用空间局部性原理（访问节点 key为 50，则 key 为 55、60、62 的节点将来也可能被访问），**使用磁盘预读原理提前将这些数据读入内存，减少了磁盘 IO 的次数。B+树也能很好的完成范围查询。比如查询 key 值在 50-70 之间的节点。**（B+树的叶子节点的数据都是使用链表连接起来的，而且在磁盘里是顺序存储，所以当读到某个值的时候，磁盘预读原理会提前把这些数据都读进内存，使得范围查询和排序都很快）

4. **B+树更适合外部存储。由于内节点无 data 域，每个节点能索引的范围更大更精确**（B树的节点都存了key和data，而B+树只有叶子节点存data，非叶子节点都只是索引值，没有实际的数据，所以B+树在一次IO里面，能读出的索引值更多，减少查询时候需要的IO次数）

## 索引

### 1. MyISAM和InnoDB的索引方式以及区别与选择

索引：一种帮助mysql高效的获取数据的数据结构，这些数据结构以某种方式引用数据。可简单理解为排好序的快速查找数据结构。作为索引中最为常见的一种类型，B-Tree索引大都采用的是 B+Tree数据结构来存储数据（NDB集群存储引擎内部实际上采用 T-Tree结构存储这种索引）。B-Tree通常也意味着所有的值都是按顺序存储的。

B-Tree数据结构：磁盘顺序读写时才能达到其宣传的数值(fio可以进行简单的读写测试)，因为随机读写需要寻址时间，则如果将索引tree构建的层数越低，使得key相近的数据都存在一起，伴随磁盘预读特性，能更进一步提高性能。

使用B+Tree的关键就是Tree层数低(3层)，有序的数据存储位置接近，结合磁盘顺序读写、OS预读写特性，使得能很快定位到数据；而使用RB-Tree时key值相近的数据会存储的较远，导致效率低下。

**索引的效率依赖与磁盘 IO 的次数，快速索引需要有效的减少磁盘 IO 次数**，如何快速索引呢？索引的原理其实是不断的缩小查找范围。平衡二叉树是每次将范围分割为两个区间。为了更快，**B-树每次将范围分割为多个区间，区间越多，定位数据越快越精确。那么如果节点为区间范围，每个节点就较大了**。所以新建节点时，直接申请页大小的空间（磁盘存储单位是按 block 分的，一般为 512 Byte。**磁盘 IO 一次读取若干个 block，称为一页**，具体大小和操作系统有关，一般为 4 k，8 k或 16 k），计算机内存分配是按页对齐的，这样就实现了一个节点只需要一次 IO。

## MyISAM索引的实现

MyISAM索引文件和数据文件分离，索引文件仅保存记录所在页的指针（物理位置），通过该值，存储引擎进行回表查询，得到一行完整记录。每个叶子页也保存了指向下一个叶子页的指针。从而方便叶子节点的范围遍历。下图是MyISAM的索引原理图：（为了简化，一个页内只存放了两条记录。）

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210509232845.png)

对于二级索引，在 MyISAM存储引擎中以与上图同样的方式实现。

## InnoDB索引的实现

1. 聚簇（集）索引

InnoDB 也采用 B+Tree实现 B-Tree索引，但使**聚簇（集）索引**的数据存储方式实现B-Tree索引。**聚簇（集）**指数据行和相邻的键值紧凑地存储在一起。InnoDB 只能聚集一个叶子页（16K）的记录（即聚簇（集）索引满足一定的范围的记录），因此包含相邻键值的记录可能会相距甚远。

- 主键索引既存储索引值,又在叶子中存储行的数据
- 如果没有主键, 则会Unique key做主键
-  如果没有unique,则系统生成一个内部的rowid做主键

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510165536.png)

2. 辅助索引

辅助索引InnoDB采用的方式是在叶子页中保存主键值，通过这个主键值回表查询到一条完整记录，因此按辅助索引检索实际上进行了二次查询。

![](https://raw.githubusercontent.com/holdthebreath/picture-bed/master/20210510171845.png)

3. 使用

InnoDB的索引不建议使用过长的字段作为主键，因为所有辅助索引都引用主索引，过长的主索引会令辅助索引变得过大。用非单调的字段（身份证号，名字这种）作为主键在InnoDB中不是个好主意，因为InnoDB数据文件本身是一颗B+Tree，非单调的主键会造成在插入新记录时数据文件为了维持B+Tree的特性而频繁的分裂调整，十分低效。应该使用自增字段作为主键。

4. 查询几次磁盘io

使用主键索引遍历一次b+，使用辅助索引遍历两次。

b+遍历是O(logn)，底数（分支数）非常大。

在实际的查询中，IO次数可能会更小，因为有可能会把部分用到的索引读取到内存中，相对于磁盘IO来说，内存的io消耗可以忽略不计。一般来说B+Tree的高度一般都在2-4层，MySQL的InnoDB存储引擎在设计时是将根节点常驻内存的，也就是说查找某一键值的行记录时最多只需要1~3次磁盘I/O操作（根节点的那次不算磁盘I/O）。

## 设计索引原则

1. 主键选自增不用uuid。（`UUID`是无序的，**MySQL在维护聚簇索引的时候都是按照主键的顺序排序的**）
2. 为频繁查找的列做索引。
3. 避免为大字段做索引。（尽量使用数据量小的字段作为索引，对长字段使用前缀索引）
4. 选择区分度大的作为索引。（select count(distinct(属性))/count(*)  from 表）
5. 尽量为order by 和 group by后面的字段建立索引
6. 不要建立太多索引
7. 频繁增删改的字段不要建立索引

### 常见索引失效

1. 使用 `OR` 关键字会导致索引失效，不过如果要想使用OR 又不想让索引失效，那就得需要为`or`条件中的每个列都建立索引。这很显然是和上面的不要建立太多的索引相违背。
2. 联合索引如果不遵循**最左前缀原则**，那么索引也将失效。
3. 使用模糊查询的时候以%开头也会导致索引失效
4. 索引列如果使用了隐式转换也会导致索引失效

## mysql优化

1. **不要对索引的字段进行函数操作**（导致不使用索引）
2. 所有的where条件查询的时候，都要确保**查询的where字段不为空**，要有这个判断的步骤，不然sql会报错。其次是要尽可能的处理数据库中的字段，因为有的时候数据库存储的字段可能不规范，所以要尽量处理这些不规范的字段，保证程序的稳定性。

3. 每次写完功能之后，要对复杂的sql进行explain查看，明确索引有没有用到，extra里面提示信息是什么，是否需要优化，一般来说索引能用到，type能达到range以上就可以，如果Extra里面含有 Using filesort或者 Using temporary 则代表该sql需要进行优化。

4.  表设计时存放大数据的字段需要拆分到其他表中，或者是考虑细化存储其中的数据。因为innodb引擎中。对于大字段如text blob等，只会存放768个字节在数据页中，而剩余的数据会存储在溢出段中，在查询这些大字段的时候，会去访问很多页，从而影响sql的性能。 最大768字节的作用是便于创建前缀索引/prefix index，其余更多的内容存储在额外的page里，哪怕只是多了一个字节。因此，所有列长度越短越好（能用 varchar 就别用text)。

影响sql速度的原因： mysql的 io 以page为单位，因此不必要的数据（大字段）也会随着需要操作的数据一同被读取到内存中来，这样带来的问题由于大字段会占用较大的内存（相比其他小字段），使得内存利用率较差，造成更多的随机读取。


5. 单张表索引数量建议控制在`5`个以内，互联网高并发业务，太多索引会影响写性能。

## Explain

当`Explain` 与 `SQL`语句一起使用时，`MySQL` 会显示来自优化器关于SQL执行的信息。也就是说，`MySQL`解释了它将如何处理该语句，包括如何连接表以及什么顺序连接表等。

`Explain` 执行计划包含字段信息如下：分别是 `id`、`select_type`、`table`、`partitions`、`type`、`possible_keys`、`key`、`key_len`、`ref`、`rows`、`filtered`、`Extra` 12个字段。

### id

表示查询中执行select子句或者操作表的顺序，**`id`的值越大，代表优先级越高，越先执行**。

1. id相同：id都相同，可以理解成查询表为一组，具有同样的优先级，执行顺序由上而下，具体顺序由优化器决定。
2. id不同：`SQL` 中存在子查询，那么 `id`的序号会递增，`id`值越大优先级越高，越先被执行 。表依次嵌套，最里层的子查询 `id`最大，最先执行。
3. 两种情况同时存在：相同`id`划分为一组，同组的从上往下顺序执行；不同组 `id`值越大，优先级越高，越先执行。

### select_type

`select_type`：表示 `select` 查询的类型，主要是用于区分各种复杂的查询，例如：`普通查询`、`联合查询`、`子查询`等。

1. `SIMPLE`：表示最简单的 select 查询语句，也就是在查询中不包含子查询或者 `union`交并差集等操作。
2. `PRIMARY`：当查询语句中包含任何复杂的子部分，最外层查询则被标记为`PRIMARY`。
3. `SUBQUERY`：当 `select` 或 `where` 列表中包含了子查询，该子查询被标记为：`SUBQUERY` 。
4. `DERIVED`：表示包含在`from`子句中的子查询的select，在我们的 `from` 列表中包含的子查询会被标记为`derived` 。
5. `UNION`：如果`union`后边又出现的`select` 语句，则会被标记为`union`；若 `union` 包含在 `from` 子句的子查询中，外层 `select` 将被标记为 `derived`。
6. `UNION RESULT`：代表从`union`的临时表中读取数据，而`table`列的`<union1,4>`表示用第一个和第四个`select`的结果进行`union`操作。

### table

查询的表名，并不一定是真实存在的表，有别名显示别名，也可能为临时表。

### partitions

查询时匹配到的分区信息，对于非分区表值为`NULL`，当查询的是分区表时，`partitions`显示分区表命中的分区情况。

### type

`type`：查询使用了何种类型，它在 `SQL`优化中是一个非常重要的指标。

性能排序：

`system` > `const` > `eq_ref` > `ref` > `ref_or_null` > `index_merge` > `unique_subquery` > `index_subquery` > `range` > `index` > `ALL`

#### system

`system`： 当表仅有一行记录时(系统表)，数据量很少，往往不需要进行磁盘IO，速度非常快。

##### const

`const`：表示查询时命中 `primary key` 主键或者 `unique` 唯一索引，或者被连接的部分是一个常量(`const`)值。这类扫描效率极高，返回数据量少，速度非常快。

##### eq_ref

`eq_ref`：查询时命中主键`primary key` 或者 `unique key`索引， `type` 就是 `eq_ref`。

##### ref

`ref`：区别于`eq_ref` ，`ref`表示使用非唯一性索引，会找到很多个符合条件的行。

##### ref_or_null

`ref_or_null`：这种连接类型类似于 ref，区别在于 `MySQL`会额外搜索包含`NULL`值的行。

##### index_merge

`index_merge`：使用了索引合并优化方法，查询使用了两个以上的索引。

##### unique_subquery

`unique_subquery`：替换下面的 `IN`子查询，子查询返回不重复的集合。

```sql
value IN (SELECT primary_key FROM single_table WHERE some_expr)
```

##### index_subquery

`index_subquery`：区别于`unique_subquery`，用于非唯一索引，可以返回重复值。

```sql
value IN (SELECT key_column FROM single_table WHERE some_expr)
```

##### range

`range`：使用索引选择行，仅检索给定范围内的行。简单点说就是针对一个有索引的字段，给定范围检索数据。在`where`语句中使用 `bettween...and `、`<`、`>`、`<=`、`in` 等条件查询 `type` 都是 `range`。

##### index

`index`：`Index` 与`ALL` 其实都是读全表，区别在于`index`是遍历索引树读取，而`ALL`是从硬盘中读取。

##### ALL

`ALL`：将遍历全表以找到匹配的行，性能最差。

### possible_keys

`possible_keys`：表示在`MySQL`中通过哪些索引，能让我们在表中找到想要的记录，一旦查询涉及到的某个字段上存在索引，则索引将被列出，**但这个索引并不定一会是最终查询数据时所被用到的索引**。

### key

`key`：区别于`possible_keys`，key是查询中实际使用到的索引，若没有使用索引，显示为`NULL`。当 `type` 为 `index_merge` 时，可能会显示多个索引。

### key_len

`key_len`：表示查询用到的索引长度（字节数），原则上长度越短越好 。

- 单列索引，那么需要将整个索引长度算进去；
- 多列索引，不是所有列都能用到，需要计算查询中实际用到的列。

注意：`key_len`只计算`where`条件中用到的索引长度，而排序和分组即便是用到了索引，也不会计算到`key_len`中。

### ref

`ref`：常见的有：`const`，`func`，`null`，字段名。

- 当使用常量等值查询，显示`const`，
- 当关联查询时，会显示相应关联表的`关联字段`
- 如果查询条件使用了`表达式`、`函数`，或者条件列发生内部隐式转换，可能显示为`func`
- 其他情况`null`

### rows

`rows`：以表的统计信息和索引使用情况，估算要找到我们所需的记录，需要读取的行数。

这是评估`SQL` 性能的一个比较重要的数据，`mysql`需要扫描的行数，很直观的显示 `SQL` 性能的好坏，一般情况下 `rows` 值越小越好。

### filtered

`filtered` 这个是一个百分比的值，表里符合条件的记录数的百分比。简单点说，这个字段表示存储引擎返回的数据在经过过滤后，剩下满足条件的记录数量的比例。

在`MySQL.5.7`版本以前想要显示`filtered`需要使用`explain extended`命令。`MySQL.5.7`后，默认`explain`直接显示`partitions`和`filtered`的信息。

### Extra

`Extra` ：不适合在其他列中显示的信息，`Explain` 中的很多额外的信息会在 `Extra` 字段显示。

##### Using index

`Using index`：我们在相应的 `select` 操作中使用了覆盖索引，通俗一点讲就是查询的列被索引覆盖，使用到覆盖索引查询速度会非常快，`SQl`优化中理想的状态。

覆盖索引：一条 `SQL`只需要通过索引就可以返回，我们所需要查询的数据（一个或几个字段），而不必通过二级索引，查到主键之后再通过主键查询整行数据（`select *` ）（回表）。

##### Using where

`Using where`：查询时未找到可用的索引，进而通过`where`条件过滤获取所需数据，但要注意的是并不是所有带`where`语句的查询都会显示`Using where`。

##### Using temporary

`Using temporary`：表示查询后结果需要使用临时表来存储，一般在排序或者分组查询时用到。

##### Using filesort

`Using filesort`：表示无法利用索引完成的排序操作，也就是`ORDER BY`的字段没有索引，通常这样的SQL都是需要优化的。（`ORDER BY`字段有索引就会用到覆盖索引，相比执行速度快很多）

##### Using join buffer

`Using join buffer`：在我们联表查询的时候，如果表的连接条件没有用到索引，需要有一个连接缓冲区来存储中间结果。

##### Impossible where

`Impossible where`：表示在我们用不太正确的`where`语句，导致没有符合条件的行。

##### No tables used

`No tables used`：我们的查询语句中没有`FROM`子句，或者有 `FROM DUAL`子句。

##### `Extra`列的信息非常非常多，这里就不再一一列举了，详见 `MySQL`官方文档

https://dev.mysql.com/doc/refman/5.7/en/explain-output.html#jointype_index_merge

