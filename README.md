# **Check Report Project**
![](https://img.shields.io/badge/license-THU__AI-blue) ![](https://img.shields.io/badge/JAVA-1.8-red)
## Status number correspondence table in writeFile function
| 數值 | Mean|
| :----- | :-----
| -5 | Please check whether the expect online and offline of the dispatch has been removed
| -4 | The smb web is not receive the machine report
| -3 | The nest program is not upload
| -2 | The element code is failed
| -1 | Can not find the order infomation
| 0 | The element code is repeat, because can not find the dispatch order
| 2 | The element code is repeat, because the order is been finished
| 10 | The element code is repeat, because can not get the element code infomation in rel_manufacture_element
| 11 | The element code is repeat, because the element code need number enough in the order
| 12 | The element code is repeat, because the online and offline date is null in dispatch order
| 13 | The element code is repeat, because the finished date is early the expect online or over the offline date
| 999 | The column name Isfinished is null
