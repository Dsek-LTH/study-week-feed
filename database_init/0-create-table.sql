CREATE TABLE IF NOT EXISTS `studyweeks` (
    `id` int(10) unsigned NOT NULL COMMENT 'Unikt ID-nummer för läsveckorna.',
      `year` int(4) unsigned NOT NULL COMMENT 'Aktuellt år.',
      `week` int(2) unsigned NOT NULL COMMENT 'Vanlig vecka, 1 - 52.',
      `studyweek` varchar(255) NOT NULL COMMENT 'Läsvecka, 1-8 eller annan kod.'
    ) ENGINE=MyISAM AUTO_INCREMENT=595 DEFAULT CHARSET=latin1 COMMENT='Läsveckor, år för år';

    --
-- Indexes for dumped tables
--

--
-- Indexes for table `studyweeks`
--
ALTER TABLE `studyweeks`
 ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `studyweeks`
--
ALTER TABLE `studyweeks`
MODIFY `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Unikt ID-nummer för läsveckorna.',AUTO_INCREMENT=595;
