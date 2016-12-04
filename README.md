[ ru | [en](README_en.md) ]

# Калькулятор портфелей

Данные для калькулятора должны быть в виде CSV файла.
Первая строка содержит названия инструментов.
Первая колонка содержит названия периодов.
Возможные разделители полей: запятая, двоеточие, точка с заяпятой, пробел, табуляция.
Возможные разделители текста: двойная кавычка и одинарная кавычка. 
Возможные знаки десятичной точки: точка и запятая.
Пустые строки игнорируются.

Пример правильного CSV файла:

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

Пример файла с данными можно скачать из [папки builds/datas](builds/datas)

Если первая колонка содержит даты, то калькулятор сам определит их формат и
отсортирует строки по возрастанию дат. Распознаваемые форматы:

    yyyy - год, всегда 4 цифры.
    mm yyyy - месяц (1 или 2 цифры) и год (всегда 4 цифры). Разделитель - любой (3.2016 эквивалентно 03-2016).
    yyyy mm - год (всегда 4 цифры) и месяц (1 или 2 цифры). Разделитель - любой.
    dd mm yyyy - день (1 или 2 цифры), месяц (1 или 2 цифры) и год (всегда 4 цифры). Разделитель - любой.
    mm dd yyyy - месяц (1 или 2 цифры), день (1 или 2 цифры) и год (всегда 4 цифры). Разделитель - любой.
    yyyy mm dd - год (всегда 4 цифры), месяц (1 или 2 цифры) и день (1 или 2 цифры). Разделитель - любой.

Если программа по всем данным в колонке не может отличить день от месяца,
то поведение зависит от текущей локали:
    
    Русская локаль: сначала день, потом месяц
    Английская локаль: 
        Американский вариант: сначала месяц, потом день
        Остальные варианты: сначала день, потом месяц

- Кнопка "Открыть" показывает диалог выбора файлов для открытия CSV-файла с данными.
Несколько файлов могут быть выбраны одновременно. Если выбрано несколько файлов,
то они будут слиты в одну таблицу.
- Кнопка "Добавить" позволяет добавить в таблицу данные из другого CSV-файла.

Слияние происходит путем сравнения текстов в подписях строк.
Данные выравниваются по нижней границе и подписи должны совпадать снизу вверх.
Пример правильных файлов для слияния:

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


## Функции калькулятора

- Редактирование данных.
- Удаление строк и столбцов. Удаляется выделенная строка/столбец таблицы,
или последняя, если ничего не выделено.
- Добавление строк. Строка добавляется после выделенной, или в конец, если ничего не выделено.
- Нормализация данных. Если история инструментов имеет разную длину, то нажатие на кнопку
"Удалить неполные строки" обрезает историю, оставляя только те периоды, которые заполнены для всех инструментов.
- Расчет таблицы корреляций инструментов
- Расчет таблицы ковариаций инструментов
- Графическое представление возможных портфелей

## Работа с графиком портфелей

- Таблица ограничений. Позволяет задать минимальные и максимальные
веса инструментов (в проценах) для расчета возможных портфелей.
- Если в таблице ограничений в строке "Сравнить" сумма весов равна 100,
то будет дополнительно нарисован портфель с этими весами (зеленым цветом).
Эта возможность позволяет проследить эволюцию портфеля за период.
- Выпадающие списки "От" и "До" позволяют задать интервал, в котором будет рассчитана доходность.
Если значение поля "до" меньше минимального периода, в кором доступны все данные,
то берется минимальный период.
- Кнопка "Только граница". Переключает режим отображения:
все портфели или только портфели на границе эффективности
- Кнопка "Ребалансировки" раскрашивает график в зависимости от доходности портфеля с ребалансировками.
- Кнопка "Нарисовать". Обновить информацию на графике.
- При движении мыши подсвечивается ближайший портфель.
- Нажатие левой кнопки открывает график доходности.
- Движение мыши с зажатой левой кнопкой позволяет увеличить масштаб.
- Нажатие правой кнопки вызывает конекстное меню, в котором подсвеченный портфель можно
выбрать для сравнения (в таблице ограничений) или посмотреть его состав.
- Кнопка "Повысить точность" граница пересчитывается более точно.
- Кнопка "Максимальная точность" пересчитывает границу максимально точно, за несколько итераций.

При переборе программа адаптируется к количеству инструментов и ограничениям на веса.
В зависимости от этого регулируется шаг перебора весов портфелей. Если инструментов в таблице много,
то расчеты становятся менее точными.

Если в расчет включены инструменты с разным доступным периодом данных, берется минимальный период,
в котором доступны все данные.

## Работа с графиком доходности
- Поле "Прогноз на период". Задает количество периодов после последнего, которые будут смоделированы.
- Флажки 1σ, 2σ, 3σ - показывают границы риска.
- Кнопка "Состав портфеля" - показывает характеристики портфеля.
- Кнопка "Логарифмическая шкала" - изменяет тип вертикальной шкалы (линейная / логарифмическая).
- Кнопка "Ребалансировки" показывает доходность портфеля с ребалансировками.
- Кнопка "Прогноз" доходность портфеля в сравнении с моделью.

## Установка и запуск

- Для работы программы требуется Java 8 или выше: https://java.com/ru/download/
- Скачайте архив программы из [папки builds](builds/).
- Распакуйте в любую папку на компьютере.
- Под Windows: запустите `calcaa.cmd`
(возможно, его придется разблокировать в свойствах файла)
- Под Linux: запустите `./calcaa.sh`
(возможно ему придется дать права на выполнение: `chmod +x calcaa.sh`)

## Поблагодарить автора
Устно:
[в ЖЖ + ответы на вопросы](http://oppositus.livejournal.com/408547.html)
[на Smatr-Lab'е](http://smart-lab.ru/blog/366844.php)

Денежно:
- [Яндекс.Деньги](https://money.yandex.ru/to/4100172000860)
- [PayPal](paypal.me/oppositus)
- BitCoin 1MpLhbZ8FqVhk2dnZYUYUDNL6o3m7UYgkm

## Лицензия

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
