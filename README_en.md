[ [ru](README.md) | en ]

# Portfolio calculator

You should have data for the calculator in CSV file.
First row contains instruments names.
First column contains periods names.
Possible fields delimiters: comma, colon, semicolon, space, tabulation.
Possible text marks: double quote and quote. 
Possible decimal points: point and comma.
Empty lines are ignored.

Example of correct CSV file:

    " ";Australia;Europe;Japan;USA
    "2001";100;100;100;100
    "2002";107,423;86,952;84,422;100,927
    "2003";112,113;107,003;125,416;111,027
    "2004";149,009;119,526;277,376;125,796
    "2005";180,77;106,272;216,875;102,227
    "2006";201,22;77,731;178,192;70,678
    "2007";189,51;106,254;208,215;92,236
    "2008";212,575;94,726;255,495;109,482
    "2009";217,178;111,438;289,226;96,119
    "2010";242,447;131,098;434,091;96,508
    "2011";279,772;142,095;374,827;104,383
    "2012";236,091;152,798;478,795;128,466
    "2013";180,638;128,672;545,076;116,52
    "2014";157,098;128,701;532,645;134,15
    "2015";149,434;150,996;655,431;156,432
    "2016";138,672;146,816;758,51;158,006

You can download an example files from the [builds/datas folder](builds/datas)

If 1st column contains dates then the calculator will determine its format and
sort rows in ascending order. This is the list of supported formats:

    yyyy - year, always 4 digits.
    mm yyyy - month (1 or 2 digits) and year (always 4 digits). The delimiter may be any (3.2016 equals to 03-2016).
    yyyy mm - year (always 4 digits) and month (1 or 2 digits). The delimiter may be any.
    dd mm yyyy - day (1 or 2 digits), month (1 or 2 digits) and year (always 4 digits). The delimiter may be any.
    mm dd yyyy - month (1 or 2 digits), day (1 or 2 digits) and year (always 4 digits). The delimiter may be any.
    yyyy mm dd - year (always 4 digits), month (1 or 2 digits) and day (1 or 2 digits). The delimiter may be any.

If the application can't distinguish day from month using the whole column,
it's behavior depends on current locale:
    
    Russian locale: first is day, second is month
    English locale: 
        American variant: first is month, second is day
        Other variants: first is day, second is month

- "Open" button allows you to open a CSV file with data. Multiple files can be selected.
In the case of multiple files are selected they will be merged.
- "Merge" button adds data from another CSV file to the current table.

Merging is done by comparing text labels for data rows. Data is aligned by bottom
and labels must match from bottom to top. The example of the valid files for merging:

    msci_year_korea_brasil.csv      msci_year_russia.csv
    
     ;Korea;Brasil
    31.12.1987;100;100
    30.12.1988;194,002;193,081
    29.12.1989;194,835;258,685
    31.12.1990;139,363;89,166
    31.12.1991;115,605;243,548
    31.12.1992;115,611;255,82
    31.12.1993;149,238;448,352       ;Russia
    30.12.1994;182,257;734,482      30.12.1994;100
    29.12.1995;173,813;578,125      29.12.1995;71,999
    31.12.1996;107,099;797,876      31.12.1996;180,763
    31.12.1997;35,075;984,389       31.12.1997;382,434
    31.12.1998;83,318;550,538       31.12.1998;64,408
    31.12.1999;158,449;889,491      31.12.1999;222,982
    29.12.2000;78,674;763,19        29.12.2000;155,225
    31.12.2001;114,841;597,056      31.12.2001;237,762
    31.12.2002;123,371;395,362      31.12.2002;270,735
    31.12.2003;163,589;801,998      31.12.2003;461,107
    31.12.2004;196,24;1046,551      31.12.2004;479,901
    30.12.2005;302,755;1569,439     30.12.2005;813,412
    29.12.2006;336,677;2205,429     29.12.2006;1250,283
    31.12.2007;437,522;3867,159     31.12.2007;1536,372
    31.12.2008;193,085;1638,168     31.12.2008;397,021
    31.12.2009;327,118;3624,512     31.12.2009;795,317
    31.12.2010;409,853;3761,353     31.12.2010;931,99
    30.12.2011;357,222;2826,654     30.12.2011;736,762
    31.12.2012;429,222;2727,71      31.12.2012;807,527
    31.12.2013;442,514;2218,127     31.12.2013;786,89
    31.12.2014;386,693;1832,343     31.12.2014;404,92
    31.12.2015;356,002;1036,234     31.12.2015;404,732

## Calculator functions

- Data editing.
- Removing rows and columns. The selected row/column is removed or the last row/column
if there is no selection.
- Adding rows. The row is added after selected one or to the end if there is no selection.
- Data normalization. If histories have different length then pressing "Delete partial rows" will remove
rows with incomplete data .
- Calculate instruments correlation.
- Calculate instruments covariance.
- Graphical view of portfolios.

## Working with portfolio chart

- Table of limitations. Allows to set min and max weights for instruments in the portfolio (in percents).
- If the row labeled "Compare" has summary weights of 100,
then additional portfolio with those weight will be drawn (in green color).
This feature allows watching portfolio evolution.
- Drop down lists "From" and "To" set the interval for which yields will be calculated.
If "From" value is less than the minimal date with full instruments history,
the minimal date will be used.
- "Frontier only" button switches drawing mode: all portfolios or efficient-frontier portfolios.
- "Rebalances" button colorizes chart depending on portfolio performance with rebalances.
- "Draw" button redraws the chart.
- The nearest portfolio is highlighted then the mouse is moved.
- Left mouse button click opens yields chart for the portfolio.
- Dragging the mouse with left button pressed allows zooming the chart.
- Right mouse button click shows popup menu where user can select to use highlighted portfolio
for comparing or to view it's components.
- "Increase accuracy" button recalculate efficient frontier with more accuracy.
- "Maximize accuracy" button recalculate efficient frontier with maximum accuracy.

When drawing portfolios the application is behaves depending on instruments count and weights limitations.
Depending on its the step between portfolios is selected. If there are too many instruments then calculations
will be less accurate.

If there are instruments with different history length then table will be filtered
to include maximum available history.

## Working with yields chart
- Field "Forecast for" sets a length of the forecast for the portfolio.
- Checkboxes 1σ, 2σ, 3σ enables drawing risk deviations.
- "Portfolio components" button shows a table with portfolio components.
- "Logarithmic scale" button switch vertical scale type.
- "Rebalances" button shows portfolio yields with rebalances.
- "Forecast" button shows portfolio yields comparing to model yields.

## Installing and launching

- Java 8 (or higher) is required for the application: https://java.com/ru/download/
- Download a zip archive from [builds folder](builds/).
- Unzip to disk.
- For Windows: launch `calcaa.cmd`
(it is possible you need to open properties and unlock the file)
- For Linux: launch `./calcaa.sh`
(maybe you need to add executable rights: `chmod +x calcaa.sh`)

## Say thanks to author
Discussion:
[LiveJournal](http://oppositus.livejournal.com/408547.html)
[Smart-Lab](http://smart-lab.ru/blog/366844.php)

Donation:
- [Yandex.Money](https://money.yandex.ru/to/4100172000860)
- [PayPal](paypal.me/oppositus)
- BitCoin 1MpLhbZ8FqVhk2dnZYUYUDNL6o3m7UYgkm

## License

    MIT License
    
    Copyright (c) [2016] [Denis Morosenko]
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.
