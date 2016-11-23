# Калькулятор портфелей

Данные для калькулятора должны быть в виде CSV файла.
Первая строка содержит названия инструментов.
Первая колонка содержит названия периодов.
Возможные разделители полей: запятая, точка с заяпятой, пробел, табуляция.
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

*Калькулятор не поддерживает расчет портфелей, если инструменты в нем имеют разную длину истории.*

## Функции калькулятора

- Редактирование данных.
- Удаление строк. Удаляется выделенная строка таблицы, или последняя, если ничего не выделено.
- Добавление строк. Строка добавляется после выделенной, или в конец, если ничего не выделено.
- Расчет таблицы корреляций инструментов
- Расчет таблицы ковариаций инструментов
- Графическое представление возможных портфелей

## Работа с графиком портфелей

- Таблица ограничений. Позволяет задать минимальные и максимальные
веса инструментов (в проценах) для расчета возможных портфелей.
- Кнопка "Только граница". Переключает режим отображения:
все портфели или только портфели на границе эффективности
- Кнопка "Нарисовать". Обновить информацию на графике.
- При движении мыши подсвечивается ближайший портфель. Нажатие левой кнопки показывает его состав.

При переборе программа адаптируется к количеству инструментов и ограничениям на веса.
В зависимости от этого регулируется шаг перебора весов портфелей. Если инструментов в таблице много,
то расчеты становятся менее точными.

## Установка и запуск

- Для работы программы требуется Java 8 или выше: https://java.com/ru/download/
- Скачайте архив программы из [папки builds](builds/).
- Распакуйте в любую папку на компьютере.
- Под Windows: запустите `calcaa.cmd`
- Под Linux: запустите `./calcaa.sh`
(возможно ему придется дать права на выполнение: `chmod +x calcaa.sh`)

## Поблагодарить автора
Устно: [в ЖЖ + ответы на вопросы](http://oppositus.livejournal.com/408547.html)

Денежно:
- [Яндекс.Деньги](https://money.yandex.ru/to/4100172000860)
- [PayPal](paypal.me/oppositus)

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
