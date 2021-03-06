-- Use forein keys support
PRAGMA foreign_keys = ON;
PRAGMA legacy_file_format = OFF;

-- Create tables
CREATE TABLE "INSTRUMENTS" (
	"INSTRUMENT"    INTEGER     PRIMARY KEY     NOT NULL,
	"TICKER"        TEXT                        NOT NULL    UNIQUE
	                                                            ON CONFLICT ABORT,
    "NAME"          TEXT                        NOT NULL,
    "CLASS"         INTEGER                     NOT NULL    REFERENCES "CLASSES" ("CLASS")
	                                                            ON DELETE RESTRICT
	                                                            ON UPDATE CASCADE,
	"DOWNLOADER"    INTEGER                     NOT NULL    REFERENCES "DOWNLOADERS" ("DOWNLOADER")
	                                                            ON DELETE RESTRICT
	                                                            ON UPDATE CASCADE,
	"SINCE"         TEXT,
	"UPDATED"       TEXT
);

CREATE TABLE "DOWNLOADERS" (
	"DOWNLOADER"    INTEGER     PRIMARY KEY     NOT NULL,
	"NAME"          TEXT                        NOT NULL,
	"URI"           TEXT                        NOT NULL
);

CREATE TABLE "CLASSES" (
	"CLASS"         INTEGER     PRIMARY KEY     NOT NULL,
	"NAME"          TEXT                        NOT NULL    UNIQUE
	                                                            ON CONFLICT ABORT
);

CREATE TABLE "DATA" (
	"ID"            INTEGER     PRIMARY KEY     NOT NULL,
	"INSTRUMENT"    INTEGER                     NOT NULL    REFERENCES "INSTRUMENTS" ("INSTRUMENT")
	                                                            ON DELETE CASCADE
	                                                            ON UPDATE CASCADE,
	"DATE"          TEXT                        NOT NULL,
	"OPEN"          REAL                        NOT NULL,
	"HIGH"          REAL                        NOT NULL,
	"LOW"           REAL                        NOT NULL,
	"CLOSE"         REAL                        NOT NULL,
	"CLOSEADJ"      REAL                        NOT NULL,
	"VOLUME"        REAL                        NOT NULL
);

CREATE TABLE "VERSION" (
	"VER"          TEXT
);

-- Create indexes
CREATE INDEX "INSTRUMENTS_DOWNLOADER" ON "INSTRUMENTS" ("DOWNLOADER");
CREATE INDEX "INSTRUMENTS_CLASS" ON "INSTRUMENTS" ("CLASS");
CREATE INDEX "INSTRUMENTS_SINCE" ON "INSTRUMENTS" ("SINCE");
CREATE UNIQUE INDEX "DATA_UNIQ" ON "DATA" ("INSTRUMENT", "DATE");

-- Create data
INSERT INTO "VERSION" VALUES
    ('2.2');

INSERT INTO "CLASSES" VALUES
    (1, 'ETF'),
    (2, 'FUND');

INSERT INTO "DOWNLOADERS" VALUES
    (1, 'Yahoo Finance', 'https://finance.yahoo.com/'),
    (2, 'Moscow Exchange', 'http://moex.com/');

INSERT INTO "INSTRUMENTS" VALUES
    (NULL, 'SPY', 'SPDR S&P 500 ETF', 1, 1, '1993-01-29', NULL),
    (NULL, 'IVV', 'iShares Core S&P 500 ETF', 1, 1, '2000-05-19', NULL),
    (NULL, 'VTI', 'Vanguard Total Stock Market ETF', 1, 1, '2001-06-15', NULL),
    (NULL, 'EFA', 'iShares MSCI EAFE ETF', 1, 1, '2001-08-27', NULL),
    (NULL, 'VOO', 'Vanguard S&P 500 ETF', 1, 1, '2010-09-09', NULL),
    (NULL, 'VWO', 'Vanguard FTSE Emerging Markets ETF', 1, 1, '2005-03-10', NULL),
    (NULL, 'VEA', 'Vanguard FTSE Developed Markets ETF', 1, 1, '2007-07-26', NULL),
    (NULL, 'QQQ', 'PowerShares QQQ ETF', 1, 1, '1999-03-10', NULL),
    (NULL, 'IWM', 'iShares Russell 2000 ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'IJH', 'iShares Core S&P Mid-Cap ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'IWD', 'iShares Russell 1000 Value ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'IWF', 'iShares Russell 1000 Growth ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'EEM', 'iShares MSCI Emerging Markets ETF', 1, 1, '2003-04-14', NULL),
    (NULL, 'VTV', 'Vanguard Value ETF', 1, 1, '2004-01-30', NULL),
    (NULL, 'IJR', 'iShares Core S&P Small-Cap ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'XLF', 'Financial Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'VUG', 'Vanguard Growth ETF', 1, 1, '2004-01-30', NULL),
    (NULL, 'VIG', 'Vanguard Dividend Appreciation ETF', 1, 1, '2006-05-02', NULL),
    (NULL, 'MDY', 'SPDR S&P MIDCAP 400 ETF', 1, 1, '1995-08-18', NULL),
    (NULL, 'IEMG', 'iShares Core MSCI Emerging Markets ETF', 1, 1, '2012-10-24', NULL),
    (NULL, 'XLE', 'Energy Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'VYM', 'Vanguard High Dividend Yield ETF', 1, 1, '2006-11-16', NULL),
    (NULL, 'DVY', 'iShares Select Dividend ETF', 1, 1, '2003-11-07', NULL),
    (NULL, 'IWB', 'iShares Russell 1000 ETF', 1, 1, '2000-05-19', NULL),
    (NULL, 'VO', 'Vanguard Mid-Cap ETF', 1, 1, '2004-01-30', NULL),
    (NULL, 'VB', 'Vanguard Small-Cap ETF', 1, 1, '2004-01-30', NULL),
    (NULL, 'SDY', 'SPDR S&P Dividend ETF', 1, 1, '2005-11-15', NULL),
    (NULL, 'EWJ', 'iShares MSCI Japan ETF', 1, 1, '1996-03-18', NULL),
    (NULL, 'IVW', 'iShares S&P 500 Growth ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'IEFA', 'iShares Core MSCI EAFE ETF', 1, 1, '2012-10-24', NULL),
    (NULL, 'VEU', 'Vanguard FTSE All-World ex-US ETF', 1, 1, '2007-03-08', NULL),
    (NULL, 'IWR', 'iShares Russell Mid-Cap ETF', 1, 1, '2001-08-27', NULL),
    (NULL, 'DIA', 'SPDR Dow Jones® Industrial Average ETF', 1, 1, '1998-01-20', NULL),
    (NULL, 'XLV', 'Health Care Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'XLK', 'Technology Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'USMV', 'iShares Edge MSCI Min Vol USA ETF', 1, 1, '2011-10-20', NULL),
    (NULL, 'IVE', 'iShares S&P 500 Value ETF', 1, 1, '2000-05-26', NULL),
    (NULL, 'RSP', 'Guggenheim S&P 500® Equal Weight ETF', 1, 1, '2003-05-01', NULL),
    (NULL, 'XLY', 'Consumer Discretionary Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'XLI', 'Industrial Select Sector SPDR Fund', 1, 1, '1998-12-22', NULL),
    (NULL, 'VGK', 'Vanguard FTSE Europe ETF', 1, 1, '2005-03-10', NULL),
    (NULL, 'VGT', 'Vanguard Information Technology ETF', 1, 1, '2004-01-30', NULL),
    (NULL, 'AGG', 'iShares Core U.S. Aggregate Bond ETF', 1, 1, '2003-09-29', NULL),
    (NULL, 'BND', 'Total Bond Market ETF', 1, 1, '2007-04-10', NULL),
    (NULL, 'LQD', 'iShares iBoxx', 1, 1, '2002-07-30', NULL),
    (NULL, 'TIP', 'iShares TIPS Bond ETF', 1, 1, '2003-12-05', NULL),
    (NULL, 'BSV', 'Short-Term Bond ETF', 1, 1, '2007-04-10', NULL),
    (NULL, 'HYG', 'iShares iBoxx', 1, 1, '2007-04-11', NULL),
    (NULL, 'VCSH', 'Vanguard Short-Term Corporate Bond ETF', 1, 1, '2009-11-23', NULL),
    (NULL, 'JNK', 'SPDR Barclays Capital High Yield Bond ETF', 1, 1, '2007-12-04', NULL),
    (NULL, 'CSJ', 'iShares 1-3 Year Credit Bond ETF', 1, 1, '2007-01-11', NULL),
    (NULL, 'BIV', 'Intermediate-Term Bond ETF', 1, 1, '2007-04-10', NULL),
    (NULL, 'SHY', 'iShares 1-3 Year Treasury Bond ETF', 1, 1, '2002-07-30', NULL),
    (NULL, 'VCIT', 'Vanguard Intermediate-Term Corporate Bond ETF', 1, 1, '2009-11-23', NULL),
    (NULL, 'GLD', 'SPDR Gold Shares ETF', 1, 1, '2004-11-18', NULL),
    (NULL, 'IAU', 'iShares Gold Trust ETF', 1, 1, '2005-01-28', NULL),
    (NULL, 'SLV', 'iShares Silver Trust ETF', 1, 1, '2006-04-28', NULL),
    (NULL, 'USO', 'United States Oil Fund', 1, 1, '2006-04-10', NULL),
    (NULL, 'DBC', 'PowerShares DB Commodity Index Tracking Fund', 1, 1, '2006-02-06', NULL),
    (NULL, 'UCO', 'ProShares Ultra Bloomberg Crude Oil', 1, 1, '2008-11-25', NULL),
    (NULL, 'GSG', 'iShares S&P GSCI Commodity-Indexed Trust ETF', 1, 1, '2006-07-21', NULL),
    (NULL, 'VNQ', 'Vanguard REIT ETF', 1, 1, '2004-09-29', NULL),
    (NULL, 'IYR', 'iShares U.S. Real Estate ETF', 1, 1, '2000-06-19', NULL),
    (NULL, 'RWX', 'SPDR Dow Jones International Real Estate ETF', 1, 1, '2006-12-19', NULL),
    (NULL, 'ICF', 'iShares Cohen & Steers REIT ETF', 1, 1, '2001-02-02', NULL),
    (NULL, 'RWR', 'SPDR Dow Jones REIT ETF', 1, 1, '2001-08-27', NULL),
    (NULL, 'VNQI', 'Vanguard Global ex-U.S. Real Estate ETF', 1, 1, '2010-11-01', NULL),
    (NULL, 'SCHH', 'Schwab U.S. REIT ETF', 1, 1, '2011-01-13', NULL),
    (NULL, 'XLRE', 'Real Estate Select Sector SPDR Fund', 1, 1, '2015-10-08', NULL),
    (NULL, 'RWO', 'SPDR Dow Jones Global Real Estate ETF', 1, 1, '2008-05-22', NULL),
    (NULL, 'REM', 'iShares Mortgage Real Estate Capped ETF', 1, 1, '2007-05-04', NULL),
    (NULL, 'TLT', 'iShares 20+ Year Treasury Bond', 1, 1, '2002-07-30', NULL),
    (NULL, 'FDN', 'First Trust Dow Jones Internet ETF', 1, 1, '2006-06-23', NULL),
    (NULL, 'XBI', 'SPDR S&P Biotech ETF', 1, 1, '2006-02-06', NULL),
    (NULL, 'IEV', 'iShares Europe', 1, 1, '2000-07-28', NULL),
    (NULL, 'BNDX', 'Vanguard Total International Bond ETF', 1, 1, '2014-12-29', NULL),
    (NULL, 'TLH', 'iShares 10-20 Year Treasury Bond', 1, 1, '2007-01-11', NULL),
    (NULL, 'IEF', 'iShares 7-10 Year Treasury Bond', 1, 1, '2002-07-30', NULL),

    (NULL, 'SCRYX', 'AB Sm Cp Core', 2, 1, '2015-12-29', NULL),
    (NULL, 'JSMGX', 'Janus Triton', 2, 1, '2005-11-30', NULL),
    (NULL, 'HFMIX', 'Hartfd:MidCap', 2, 1, '2009-02-27', NULL),
    (NULL, 'JGMAX', 'Janus Triton', 2, 1, '2005-08-04', NULL),
    (NULL, 'BCSSX', 'Brown Cap Sm Co', 2, 1, '2011-12-15', NULL),
    (NULL, 'JMGRX', 'Janus Enterprise', 2, 1, '2005-11-30', NULL),
    (NULL, 'JDMAX', 'Janus Enterprise', 2, 1, '2005-01-25', NULL),
    (NULL, 'ETAGX', 'Eventide Gilead', 2, 1, '2009-10-28', NULL),
    (NULL, 'FGROX', 'Emerald:Growth', 2, 1, '2008-10-20', NULL),
    (NULL, 'BPTIX', 'Baron Partners Fund', 2, 1, '2009-05-29', NULL),
    (NULL, 'JDMNX', 'Janus Enterprise', 2, 1, '2012-07-12', NULL),
    (NULL, 'FMAGX', 'Fidelity Magellan Fund', 2, 1, '1980-01-02', NULL),
    (NULL, 'JLGRX', 'JPMorgan:LgCp Gro', 2, 1, '2009-04-14', NULL),
    (NULL, 'FUNYX', 'Pioneer Fndmntl Gro', 2, 1, '2009-04-07', NULL),
    (NULL, 'PMCPX', 'Principal:MidCap', 2, 1, '2010-09-27', NULL),
    (NULL, 'JGMNX', 'Janus Triton', 2, 1, '2012-05-31', NULL),
    (NULL, 'IMIDX', 'Congress MC Gro', 2, 1, '2012-11-01', NULL),
    (NULL, 'RNGGX', 'American Funds NEco', 2, 1, '2009-05-01', NULL),
    (NULL, 'AKRIX', 'Akre Focus Fund', 2, 1, '2009-08-31', NULL),
    (NULL, 'FAMGX', 'Fidelity Adv Srs Opp Ins', 2, 1, '2012-12-11', NULL),
    (NULL, 'FWWEX', 'Fidelity Srs Opp Ins', 2, 1, '2012-12-11', NULL),
    (NULL, 'RGAGX', 'American Funds Gro', 2, 1, '2009-05-01', NULL),
    (NULL, 'BARIX', 'Baron Asset Fund', 2, 1, '2009-05-29', NULL),
    (NULL, 'FVWSX', 'Fidelity Srs Opp Ins', 2, 1, '2012-12-11', NULL),
    (NULL, 'AMOMX', 'AQR:Lg Cap Mom Style', 2, 1, '2009-07-09', NULL),
    (NULL, 'TRMIX', 'T Rowe Price MC Vl', 2, 1, '2015-09-08', NULL),
    (NULL, 'VSIAX', 'Vanguard SC Val Idx', 2, 1, '2011-09-27', NULL),
    (NULL, 'XSLV', 'PowerShares S&P SmCp LV', 2, 1, '2013-02-15', NULL),
    (NULL, 'VMVAX', 'Vanguard MC Val Idx', 2, 1, '2011-09-27', NULL),
    (NULL, 'BOSVX', 'Bridgeway:Omni SCV', 2, 1, '2011-08-31', NULL),
    (NULL, 'BWLYX', 'Am Beacon:BW LC Val', 2, 1, '2012-02-06', NULL),
    (NULL, 'BWLIX', 'Am Beacon:BW LC Val', 2, 1, '2012-02-06', NULL),
    (NULL, 'LVMIX', 'Lord Abbett Cal MCV', 2, 1, '2011-12-22', NULL),
    (NULL, 'DSEEX', 'DoubleLine:Sh Enh CAPE', 2, 1, '2013-11-01', NULL),
    (NULL, 'ESPNX', 'WellsFargo:Spec SCV', 2, 1, '2010-07-30', NULL),
    (NULL, 'JDVWX', 'J Hancock III:Ds Val', 2, 1, '2011-08-31', NULL),
    (NULL, 'JLVMX', 'JPMorgan:LgCp Val', 2, 1, '2010-11-30', NULL),
    (NULL, 'TWQZX', 'Transam:Large Cp Val', 2, 1, '2010-11-16', NULL),
    (NULL, 'JDVNX', 'J Hancock III:Ds Val', 2, 1, '2014-06-11', NULL),
    (NULL, 'GIFFX', 'Invesco Gr & Income', 2, 1, '2012-09-24', NULL),
    (NULL, 'WSCVX', 'Walthausen:SC Value', 2, 1, '2008-02-01', NULL),
    (NULL, 'LBISX', 'LM BW Diversified LCV', 2, 1, '2010-09-07', NULL),
    (NULL, 'ACSHX', 'Invesco Comstock', 2, 1, '2010-06-01', NULL),
    (NULL, 'AMDVX', 'Amer Cent:MC Val', 2, 1, '2013-07-26', NULL),
    (NULL, 'ACGQX', 'Invesco Gr & Income', 2, 1, '2010-06-01', NULL),
    (NULL, 'FRGEX', 'Fidelity Srs Stk S LCV', 2, 1, '2012-12-10', NULL),
    (NULL, 'FMMLX', 'Fidelity Adv Srs SS LCV', 2, 1, '2012-12-10', NULL),
    (NULL, 'LCEFX', 'Invesco Dvsfd Div', 2, 1, '2012-09-24', NULL),
    (NULL, 'FBLEX', 'Fidelity Srs Stk S LCV', 2, 1, '2012-12-10', NULL),
    (NULL, 'ICSFX', 'Invesco Comstock', 2, 1, '2012-09-24', NULL),
    (NULL, 'PRVIX', 'T Rowe Price SC Val', 2, 1, '2015-08-28', NULL),
    (NULL, 'VSEMX', 'Vanguard Ext Mk Id', 2, 1, '2016-03-29', NULL),
    (NULL, 'FLCPX', 'Fidelity SAI US LC Idx', 2, 1, '2016-02-12', NULL),
    (NULL, 'MVSSX', 'Victory:Integrity SCV', 2, 1, '2012-06-01', NULL),
    (NULL, 'JVMRX', 'J Hancock III:DVMC', 2, 1, '2011-08-31', NULL),
    (NULL, 'ARAIX', 'Ariel:Fund', 2, 1, '2011-12-30', NULL),
    (NULL, 'LYRIX', 'Lyrical US Val Eqty', 2, 1, '2013-02-04', NULL),
    (NULL, 'BIAUX', 'Brown Adv SC FV', 2, 1, '2009-01-02', NULL),
    (NULL, 'VSENX', 'JPMorgan:SmCp Eqty', 2, 1, '2016-05-31', NULL),
    (NULL, 'DHMYX', 'Diamond Hill S/Md Cp', 2, 1, '2011-12-30', NULL),
    (NULL, 'FSOFX', 'Fidelity Srs Sm Cap Op', 2, 1, '2009-06-26', NULL),
    (NULL, 'CPXRX', 'Columbia:MdCp Index', 2, 1, '2012-11-09', NULL),
    (NULL, 'OTIIX', 'T Rowe Price SC Stk', 2, 1, '2015-08-28', NULL),
    (NULL, 'COFRX', 'Columbia:Cntr Core', 2, 1, '2012-11-09', NULL),
    (NULL, 'FSSVX', 'Fidelity SmCp Id', 2, 1, '2011-09-09', NULL),
    (NULL, 'SCHA', 'Schwab Str:US Sm Cap ETF', 2, 1, '2009-11-03', NULL),
    (NULL, 'RAFGX', 'American Funds AMCP', 2, 1, '2009-05-01', NULL),
    (NULL, 'XMLV', 'PowerShares S&P MidCp LV', 2, 1, '2013-02-15', NULL),
    (NULL, 'FSKAX', 'Fidelity Tot Mk', 2, 1, '2011-09-08', NULL),
    (NULL, 'FSKTX', 'Fidelity Tot Mk', 2, 1, '2011-09-08', NULL),
    (NULL, 'VSTSX', 'Vanguard TSM Idx', 2, 1, '2016-03-29', NULL),
    (NULL, 'FSTPX', 'Fidelity MdCp Id', 2, 1, '2011-09-09', NULL),
    (NULL, 'RWMGX', 'American Funds Wash', 2, 1, '2009-05-01', NULL),
    (NULL, 'FSCKX', 'Fidelity MdCp Id', 2, 1, '2011-09-09', NULL),
    (NULL, 'TILT', 'FlexShs:MS US Mkt Fac', 2, 1, '2011-09-22', NULL),
    (NULL, 'REIPX', 'T Rowe Price Eq Inc', 2, 1, '2015-12-17', NULL),
    (NULL, 'SDOG', 'Alps Sect Div Dogs ETF', 2, 1, '2012-07-16', NULL),
    (NULL, 'SPHD', 'PowerShares S&P500 HD LV', 2, 1, '2012-10-26', NULL),
    (NULL, 'PVSYX', 'Putnam Cap Spectrum', 2, 1, '2009-05-18', NULL),
    (NULL, 'QDF', 'FlexShs:Quality Div', 2, 1, '2012-12-19', NULL),
    (NULL, 'PVSAX', 'Putnam Cap Spectrum', 2, 1, '2009-05-18', NULL),
    (NULL, 'PEQSX', 'Putnam Equity Income', 2, 1, '2012-07-02', NULL),
    (NULL, 'SCHD', 'Schwab Str:US Div Eq ETF', 2, 1, '2011-10-20', NULL),
    (NULL, 'PVSCX', 'Putnam Cap Spectrum', 2, 1, '2009-05-18', NULL),
    (NULL, 'OIEJX', 'JPMorgan:Equity Inc', 2, 1, '2012-01-31', NULL),
    (NULL, 'PMDPX', 'Principal:Sm-MC DvI', 2, 1, '2011-06-07', NULL),
    (NULL, 'PMDIX', 'Principal:Sm-MC DvI', 2, 1, '2011-06-07', NULL),
    (NULL, 'TRPDX', 'T Rowe Price Ret:I2040', 2, 1, '2015-10-02', NULL),
    (NULL, 'FRLLX', 'Fidelity Srs Eqty-Inc', 2, 1, '2012-12-11', NULL),
    (NULL, 'FLMLX', 'Fidelity Adv Srs Eqty-In', 2, 1, '2012-12-10', NULL),
    (NULL, 'ABCYX', 'Am Beacon:LCo Inc Eq', 2, 1, '2012-05-29', NULL),
    (NULL, 'TRPJX', 'T Rowe Price Ret:I2035', 2, 1, '2015-10-02', NULL),
    (NULL, 'FNKLX', 'Fidelity Srs Eqty-Inc', 2, 1, '2012-12-11', NULL),
    (NULL, 'DVIPX', 'Davenport Value & Income', 2, 1, '2010-12-31', NULL),
    (NULL, 'MQIFX', 'Franklin Mut Quest', 2, 1, '1980-09-16', NULL),
    (NULL, 'OIERX', 'JPMorgan:Equity Inc', 2, 1, '2001-02-15', NULL),
    (NULL, 'HDV', 'iShares:Core High Div', 2, 1, '2011-03-31', NULL),
    (NULL, 'RLBGX', 'American Funds Bal', 2, 1, '2009-05-01', NULL),
    (NULL, 'TRPCX', 'T Rowe Price Ret:I2030', 2, 1, '2015-10-02', NULL),
    (NULL, 'RFITX', 'American Funds T2050', 2, 1, '2009-05-01', NULL),
    (NULL, 'OSCIX', 'Oppenheimer Intl SMC', 2, 1, '2011-12-29', NULL),
    (NULL, 'FERGX', 'Fidelity SAI EM Index', 2, 1, '2016-01-05', NULL),
    (NULL, 'FGLLX', 'Fidelity Srs Intr Opp', 2, 1, '2012-12-07', NULL),
    (NULL, 'FDMLX', 'Fidelity Srs Intr Opp', 2, 1, '2012-12-07', NULL),
    (NULL, 'RLLGX', 'American Funds SMCP', 2, 1, '2009-05-01', NULL),
    (NULL, 'HFEIX', 'Henderson:Euro Foc', 2, 1, '2009-03-31', NULL),
    (NULL, 'FKSCX', 'Franklin Intl SCG', 2, 1, '2002-10-21', NULL),
    (NULL, 'PEFIX', 'PIMCO:RAE Fdmtl+EMG', 2, 1, '2008-11-28', NULL),
    (NULL, 'FISMX', 'Fidelity Intl Sm Cap', 2, 1, '2002-09-19', NULL),
    (NULL, 'GPIIX', 'Grandeur Itl Opp', 2, 1, '2011-10-17', NULL),
    (NULL, 'EMRGX', 'Emerging Markets Growth', 2, 1, '2014-11-03', NULL),
    (NULL, 'ARTKX', 'Artisan:Intl Val', 2, 1, '2002-09-24', NULL),
    (NULL, 'RNPGX', 'American Funds NPer', 2, 1, '2009-05-01', NULL),
    (NULL, 'FMGEX', 'Frontier MFG Gl Eq', 2, 1, '2011-12-28', NULL),
    (NULL, 'APHGX', 'Artisan:Glbl Val', 2, 1, '2012-07-17', NULL),
    (NULL, 'OSMAX', 'Oppenheimer Intl SMC', 2, 1, '1997-11-17', NULL),
    (NULL, 'ANWPX', 'American Funds NPer', 2, 1, '1980-01-02', NULL),
    (NULL, 'TEPLX', 'Templeton Growth', 2, 1, '1986-01-02', NULL),
    (NULL, 'DBJP', 'Deutsche MSCI Jp Hdg Eq', 2, 1, '2011-06-09', NULL),
    (NULL, 'MDISX', 'Franklin Mut Gl Disc', 2, 1, '1992-12-31', NULL),
    (NULL, 'ODMAX', 'Oppenheimer Dev Mkts', 2, 1, '1996-11-18', NULL),
    (NULL, 'NMMEX', 'Northern Fds:Act M EM Eq', 2, 1, '2008-12-15', NULL),
    (NULL, 'OPGIX', 'Oppenheimer Glbl Opp', 2, 1, '1990-10-22', NULL),
    (NULL, 'TEMWX', 'Templeton World', 2, 1, '1995-08-14', NULL),
    (NULL, 'BISMX', 'Brandes Inv:Itl SC Eq', 2, 1, '2012-02-03', NULL),
    (NULL, 'XIV', 'VelShs DlyInv VIX ST ETN', 2, 1, '2010-11-30', NULL),
    (NULL, 'SPXL', 'Direxion:S&P 500 Bull 3X', 2, 1, '2008-11-05', NULL),
    (NULL, 'TRNEX', 'T Rowe Price New Era', 2, 1, '2015-12-17', NULL),
    (NULL, 'BBH', 'VnEck Vctrs:Biotech ETF', 2, 1, '1999-11-23', NULL),
    (NULL, 'TNA', 'Direxion:Sm Cap Bull 3X', 2, 1, '2008-11-19', NULL),
    (NULL, 'SMH', 'VnEck Vctrs:Semicnd ETF', 2, 1, '2000-06-05', NULL),
    (NULL, 'KBWB', 'PowerShares KBW Bank', 2, 1, '2011-11-01', NULL),
    (NULL, 'MBXIX', 'Catalyst/Millburn HS', 2, 1, '2015-12-28', NULL),
    (NULL, 'PSCT', 'PowerShares S&P SC Info', 2, 1, '2010-04-07', NULL),
    (NULL, 'VGHCX', 'Vanguard Health Care', 2, 1, '1984-05-23', NULL),
    (NULL, 'QLEIX', 'AQR:Lng-Sht Eqty', 2, 1, '2013-07-16', NULL),
    (NULL, 'FSPHX', 'Fidelity Sel Health', 2, 1, '1981-07-14', NULL),
    (NULL, 'FSCSX', 'Fidelity Sel SW & IT Svc', 2, 1, '1998-12-01', NULL),
    (NULL, 'PHSZX', 'Pru Jenn Health Sci', 2, 1, '1999-06-30', NULL),
    (NULL, 'FBGX', 'UBS AG Enh LC Growth ETN', 2, 1, '2014-06-11', NULL),
    (NULL, 'PHLAX', 'Pru Jenn Health Sci', 2, 1, '1999-06-30', NULL),
    (NULL, 'SHSAX', 'BlackRock:HS Opp', 2, 1, '1999-12-20', NULL),
    (NULL, 'BREIX', 'Baron Real Estate', 2, 1, '2009-12-31', NULL),
    (NULL, 'CGMRX', 'CGM Tr:Realty Fund', 2, 1, '1994-05-13', NULL),
    (NULL, 'PRHSX', 'T Rowe Price Hlth Sci', 2, 1, '1995-12-29', NULL),
    (NULL, 'PRMTX', 'T Rowe Price Md/Tele', 2, 1, '1993-10-13', NULL),
    (NULL, 'FSCHX', 'Fidelity Sel Chemicals', 2, 1, '1985-07-29', NULL),
    (NULL, 'PJP', 'PowerShares Dyn Pharm', 2, 1, '2005-06-23', NULL),
    (NULL, 'FSRPX', 'Fidelity Sel Retailing', 2, 1, '1985-12-16', NULL),
    (NULL, 'FSMEX', 'Fidelity Sel Med Equip', 2, 1, '1998-04-28', NULL),
    (NULL, 'MMIZX', 'MassMutual Sel:S&P500', 2, 1, '2011-12-07', NULL),
    (NULL, 'PRUIX', 'T Rowe Price Eq Idx500', 2, 1, '2015-08-28', NULL),
    (NULL, 'BSPIX', 'BlackRock:S&P500 Idx', 2, 1, '2013-04-11', NULL),
    (NULL, 'FXAIX', 'Fidelity 500 Idx', 2, 1, '2011-05-04', NULL),
    (NULL, 'FXSIX', 'Fidelity 500 Idx', 2, 1, '2011-05-04', NULL),
    (NULL, 'VFFSX', 'Vanguard 500 Index', 2, 1, '2016-03-29', NULL),
    (NULL, 'BSPAX', 'BlackRock:S&P500 Idx', 2, 1, '2013-04-11', NULL),
    (NULL, 'VFINX', 'Vanguard 500 Index', 2, 1, '1980-01-02', NULL),
    (NULL, 'WFIOX', 'WellsFargo:Index', 2, 1, '1986-08-15', NULL),
    (NULL, 'FUSEX', 'Fidelity 500 Idx', 2, 1, '1988-02-17', NULL),
    (NULL, 'SPINX', 'SEI Inst Inv:S&P500', 2, 1, '2013-12-19', NULL),
    (NULL, 'MSPIX', 'MainStay:S&P 500 Idx', 2, 1, '1990-12-31', NULL),
    (NULL, 'VINIX', 'Vanguard Instl Indx', 2, 1, '1990-07-31', NULL),
    (NULL, 'PREIX', 'T Rowe Price Eq Idx500', 2, 1, '1990-03-30', NULL),
    (NULL, 'TISPX', 'TIAA-CREF:S&P500 Idx', 2, 1, '2002-09-30', NULL),
    (NULL, 'HLEIX', 'JPMorgan:Equity Idx', 2, 1, '1991-07-02', NULL),
    (NULL, 'SSEYX', 'SS Inst Inv:Eq 500 Id II', 2, 1, '2014-08-14', NULL),
    (NULL, 'WFSPX', 'BlackRock:S&P500 Idx', 2, 1, '1993-07-02', NULL),
    (NULL, 'SVSPX', 'SSgA:S&P 500 Index', 2, 1, '1992-12-30', NULL),
    (NULL, 'DSPIX', 'Dreyfus Instl S&P 500', 2, 1, '1993-09-30', NULL),
    (NULL, 'NINDX', 'Columbia:LgCp Index', 2, 1, '1993-12-15', NULL),
    (NULL, 'TRSPX', 'TIAA-CREF:S&P500 Idx', 2, 1, '2002-09-06', NULL),
    (NULL, 'PEOPX', 'Dreyfus S&P 500 Index', 2, 1, '1989-12-29', NULL),
    (NULL, 'OGEAX', 'JPMorgan:Equity Idx', 2, 1, '1992-02-18', NULL),
    (NULL, 'GMCDX', 'GMO:Emer Ctry Dbt', 2, 1, '1994-04-19', NULL),
    (NULL, 'HWHIX', 'Hotchkis:High Yield', 2, 1, '2009-03-31', NULL),
    (NULL, 'GMDFX', 'GMO:Emer Ctry Dbt', 2, 1, '1998-01-09', NULL),
    (NULL, 'FMKIX', 'Fidelity Adv Emerg', 2, 1, '1994-03-31', NULL),
    (NULL, 'PTCIX', 'PIMCO:Lng-Tm Credit', 2, 1, '2009-03-31', NULL),
    (NULL, 'FNMIX', 'Fidelity New Mkts Inc', 2, 1, '1993-05-04', NULL),
    (NULL, 'PREMX', 'T Rowe Price Int:EM Bd', 2, 1, '1994-12-30', NULL),
    (NULL, 'AGDAX', 'AB High Income', 2, 1, '1994-02-25', NULL),
    (NULL, 'RITGX', 'American Funds HI', 2, 1, '2009-05-01', NULL),
    (NULL, 'HWHAX', 'Hotchkis:High Yield', 2, 1, '2009-05-29', NULL),
    (NULL, 'MEDIX', 'MFS Emerg Mkt Debt', 2, 1, '1998-03-17', NULL),
    (NULL, 'FAGIX', 'Fidelity Capital & Inc', 2, 1, '1980-01-02', NULL),
    (NULL, 'MEDAX', 'MFS Emerg Mkt Debt', 2, 1, '1998-03-17', NULL),
    (NULL, 'FXICX', 'PIMCO:FISH Series C', 2, 1, '2000-10-18', NULL),
    (NULL, 'NHILX', 'Neuberger Hi Inc B', 2, 1, '2009-05-27', NULL),
    (NULL, 'AGDCX', 'AB High Income', 2, 1, '1994-02-25', NULL),
    (NULL, 'PEBIX', 'PIMCO:Em Mkts Bd', 2, 1, '1997-07-31', NULL),
    (NULL, 'PONPX', 'PIMCO:Income', 2, 1, '2008-04-30', NULL),
    (NULL, 'TGEIX', 'TCW:Em Mkts Income', 2, 1, '1998-05-29', NULL),
    (NULL, 'EIHIX', 'Eaton Vance HI Opp', 2, 1, '2009-12-09', NULL),
    (NULL, 'LSBDX', 'Loomis Sayles:Bond', 2, 1, '1991-05-10', NULL),
    (NULL, 'FIHBX', 'Federated HY Bond', 2, 1, '2002-11-04', NULL),
    (NULL, 'PIMIX', 'PIMCO:Income', 2, 1, '2007-04-02', NULL),
    (NULL, 'LSFIX', 'Loomis Sayles:Fx In', 2, 1, '1995-01-17', NULL),
    (NULL, 'GSDIX', 'Goldman:Emg Mkts Dbt', 2, 1, '2003-09-02', NULL),
    (NULL, 'DVHIX', 'Delaware Natl HY', 2, 1, '2008-12-31', NULL),
    (NULL, 'PGOVX', 'PIMCO:Lng-Tm Govt', 2, 1, '1991-06-28', NULL),
    (NULL, 'MOTMX', 'BNY Mellon:Mun Opp', 2, 1, '2008-10-16', NULL),
    (NULL, 'HYD', 'VnEck Vctrs:HY Muni Indx', 2, 1, '2009-02-05', NULL),
    (NULL, 'MMHIX', 'MainStay:Hi Yld Muni', 2, 1, '2010-03-30', NULL),
    (NULL, 'SHMMX', 'WA Managed Muni', 2, 1, '1981-03-04', NULL),
    (NULL, 'VUSTX', 'Vanguard Lg-Tm Trs', 2, 1, '1986-05-19', NULL),
    (NULL, 'MMHAX', 'MainStay:Hi Yld Muni', 2, 1, '2010-03-30', NULL),
    (NULL, 'ELFTX', 'Elfun Tax-Ex Income Fund', 2, 1, '1980-01-02', NULL),
    (NULL, 'WFCMX', 'WellsFargo:CreBldr Srs M', 2, 1, '2008-04-16', NULL),
    (NULL, 'ORNYX', 'Oppenheimer Ro HY M', 2, 1, '2010-11-29', NULL),
    (NULL, 'USTEX', 'USAA Tax Ex Lng-Tm', 2, 1, '1982-03-19', NULL),
    (NULL, 'FGOVX', 'Fidelity Inc:Govt Inc', 2, 1, '1980-01-02', NULL),
    (NULL, 'MANLX', 'BlackRock:Nat Muni', 2, 1, '1980-01-02', NULL),
    (NULL, 'COLTX', 'Columbia:Tax-Exempt', 2, 1, '1980-01-02', NULL),
    (NULL, 'ABTYX', 'AB Hi Inc Muni', 2, 1, '2010-02-08', NULL),
    (NULL, 'PRFHX', 'T Rowe Price Tx-Fr HY', 2, 1, '1985-02-28', NULL),
    (NULL, 'MISHX', 'AB Municipal Inc', 2, 1, '2012-10-01', NULL),
    (NULL, 'SRHMX', 'Columbia:Hi Yld Muni', 2, 1, '1995-11-03', NULL),
    (NULL, 'CPTNX', 'Amer Cent:Govt Bond', 2, 1, '1980-05-16', NULL),
    (NULL, 'FTFMX', 'Fidelity NY Muni Inc', 2, 1, '1984-07-10', NULL),
    (NULL, 'VWAHX', 'Vanguard Hi Yld TxEx', 2, 1, '1980-01-02', NULL),
    (NULL, 'USATX', 'USAA Tax Ex Intm-Tm', 2, 1, '1982-03-19', NULL),
    (NULL, 'FKTIX', 'Franklin Fed TF Inc', 2, 1, '1983-10-07', NULL),
    (NULL, 'OPNYX', 'Oppenheimer Ro NY M', 2, 1, '1984-08-16', NULL),

    (NULL, 'FXRB', 'FinEx Tradable Russian Corporate Bonds UCITS ETF (RUB)', 1, 2, '2013-04-29.', NULL),
    (NULL, 'FXRU', 'FinEx Tradable Russian Corporate Bonds UCITS ETF (USD)', 1, 2, '2013-12-02', NULL),
    (NULL, 'FXGD', 'FinEx Gold ETF (USD)', 1, 2, '2013-10-17', NULL),
    (NULL, 'FXDE', 'FinEx MSCI Germany UCITS ETF (EUR)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXIT', 'FinEx MSCI USA Information Technology UCITS ETF (USD)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXJP', 'FinEx MSCI Japan UCITS ETF (USD)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXAU', 'FinEx MSCI Australia UCITS ETF (USD)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXUS', 'FinEx MSCI USA UCITS ETF (USD)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXUK', 'FinEx MSCI United Kingdom UCITS ETF (GBP)', 1, 2, '2013-10-31', NULL),
    (NULL, 'FXCN', 'FinEx MSCI China UCITS ETF (USD)', 1, 2, '2014-02-06', NULL),
    (NULL, 'FXMM', 'FinEx UCITS ETF (RUB)', 1, 2, '2014-05-16', NULL),
    (NULL, 'FXRL', 'FinEx Russian RTS Equity UCITS ETF (USD)', 1, 2, '2016-03-02', NULL);
