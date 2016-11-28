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

You can download an example file from the builds folder: [msci_year.csv](builds/msci_year.csv)

## Calculator functions

- Data editing.
- Removing rows. The selected row is removed or the last row if there is no selection.
- Adding rows. The row is added after selected one or to the end if there is no selection.
- Data normalization. If histories have different length then pressing "Delete partial rows" will remove
rows with incomplete data .
- Calculate instruments correlation.
- Calculate instruments covariance.
- Graphical view of portfolios.

## Working with portfolio chart

- Table of limitations. Allows to set min and max weights for instruments in the portfolio (in percents).
- "Border only" button switches drawing mode: all portfolios or effective portfolios.
- "Draw" button redraws the chart.
- The nearest portfolio is highlighted then the mouse is moved.
- Left mouse button click opens yields chart for the portfolio.
- Dragging the mouse with left button pressed allows zooming the chart.
- Left mouse button click opens portfolio components table.
- "Increase accuracy" button recalculate effective border with more accuracy.
- "Maximize accuracy" button recalculate effective border with maximum accuracy.

When drawing portfolios the application is behaves depending on instruments count and weights limitations.
Depending on its the step between portfolios is selected. If there are too many instruments then calculations
will be less accurate.

If there are instruments with different history length then table will be filtered
to include maximum available history.

## Working with yields chart
- Field "Prognosis for" sets a length of the forecast for the portfolio.
- Checkboxes 1σ, 2σ, 3σ enables risk borders.
- "Portfolio components" button shows a table with portfolio components.
- "Logarithmic scale" button switch vertical scale type.
- "Rebalances" button shows portfolio yields with rebalances.
- "Prognosis" button shows portfolio yields comparing to model yields.

## Installing and launching

- Java 8 (or higher) is required for the application: https://java.com/ru/download/
- Download a zip archive from [builds folder](builds/).
- Unzip to disk.
- For Windows: launch `calcaa.cmd`
(it is possible you need to open properties and unlock the file)
- For Linux: launch `./calcaa.sh`
(may be you need to add executable rights: `chmod +x calcaa.sh`)

## Say thanks to author
Discussion:
[LiveJournal](http://oppositus.livejournal.com/408547.html)

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
