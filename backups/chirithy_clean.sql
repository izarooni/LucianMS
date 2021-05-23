CREATE DATABASE IF NOT EXISTS `chirithy`;
USE `chirithy`;

CREATE TABLE `_donation_hashes` (
  `donation_id` int NOT NULL AUTO_INCREMENT,
  `hash` varchar(64) NOT NULL,
  `account_name` varchar(13) NOT NULL,
  `amount` int NOT NULL,
  `claimed` tinyint NOT NULL DEFAULT '0',
  `enabled` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`donation_id`),
  UNIQUE KEY `hash_UNIQUE` (`hash`),
  KEY `account_name` (`account_name`),
  CONSTRAINT `_donation_hashes_ibfk_1` FOREIGN KEY (`account_name`) REFERENCES `accounts` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=latin1;

CREATE TABLE `_web_posts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(45) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `category` varchar(45) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `content` text CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `date_posted` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;

CREATE TABLE `_web_uris` (
  `name` varchar(100) CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  `uri` text CHARACTER SET latin1 COLLATE latin1_bin NOT NULL,
  KEY `web_uri_name_idx` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `accounts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `gm` tinyint(1) NOT NULL DEFAULT '0',
  `name` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `password` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `email` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `pic` varchar(26) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `pin` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `gender` tinyint NOT NULL DEFAULT '0',
  `characterslots` tinyint NOT NULL DEFAULT '5',
  `loggedin` tinyint NOT NULL DEFAULT '0',
  `last_login` timestamp NULL DEFAULT NULL,
  `creation_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `birthday` date NOT NULL DEFAULT '1990-01-01',
  `banned` tinyint(1) NOT NULL DEFAULT '0',
  `ban_reason` text CHARACTER SET latin1 COLLATE latin1_swedish_ci,
  `temporary_ban` timestamp NULL DEFAULT NULL,
  `tos` tinyint(1) DEFAULT '1',
  `nxCredit` int DEFAULT NULL,
  `maplePoint` int DEFAULT NULL,
  `nxPrepaid` int DEFAULT NULL,
  `mute` int DEFAULT '0',
  `last_known_ip` varchar(500) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `discord_id` bigint unsigned DEFAULT NULL,
  `daily_login` timestamp NULL DEFAULT NULL,
  `daily_showable` tinyint unsigned NOT NULL DEFAULT '1',
  `sitelogged` text CHARACTER SET latin1 COLLATE latin1_swedish_ci,
  `nick` varchar(20) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `remember_token` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `login_streak` int unsigned NOT NULL DEFAULT '0',
  `webadmin` tinyint unsigned DEFAULT '0',
  `votepoints` int NOT NULL DEFAULT '0',
  `rewardpoints` int NOT NULL DEFAULT '0',
  `donationpoints` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  KEY `ranking1` (`id`,`banned`,`gm`),
  KEY `loggedin` (`loggedin`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8;

CREATE TABLE `accounts_hwid` (
  `account_id` int NOT NULL,
  `hwid` varchar(45) CHARACTER SET latin2 COLLATE latin2_bin NOT NULL,
  KEY `account_hwid_accid_idx` (`account_id`),
  CONSTRAINT `FK_accounts_hwid_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `accounts_mac` (
  `account_id` int NOT NULL,
  `mac` varchar(45) CHARACTER SET latin2 COLLATE latin2_bin NOT NULL,
  KEY `accounts_mac_accid_idx` (`account_id`),
  CONSTRAINT `FK_accounts_mac_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `achievements` (
  `completed` tinyint unsigned NOT NULL DEFAULT '0',
  `player_id` int unsigned NOT NULL,
  `achievement_name` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `killed_monster` int NOT NULL DEFAULT '0',
  `casino_one` tinyint(1) NOT NULL DEFAULT '0',
  `casino_two` tinyint(1) NOT NULL DEFAULT '0',
  KEY `achievements_playerid_idx` (`player_id`),
  CONSTRAINT `FKplayerid_achievements` FOREIGN KEY (`player_id`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `alliance` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `notice` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `capacity` int unsigned NOT NULL DEFAULT '2',
  `rank_title1` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Master',
  `rank_title2` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Jr.Master',
  `rank_title3` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `rank_title4` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `rank_title5` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `guild1` int NOT NULL DEFAULT '-1',
  `guild2` int NOT NULL DEFAULT '-1',
  `guild3` int NOT NULL DEFAULT '-1',
  `guild4` int NOT NULL DEFAULT '-1',
  `guild5` int NOT NULL DEFAULT '-1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

CREATE TABLE `arcade` (
  `id` int NOT NULL,
  `charid` int NOT NULL,
  `highscore` int NOT NULL,
  UNIQUE KEY `id` (`id`,`charid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `area_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `charid` int NOT NULL,
  `area` int NOT NULL,
  `info` varchar(200) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `bbs_replies` (
  `replyid` int unsigned NOT NULL AUTO_INCREMENT,
  `threadid` int unsigned NOT NULL,
  `postercid` int unsigned NOT NULL,
  `timestamp` bigint unsigned NOT NULL,
  `content` varchar(26) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`replyid`),
  KEY `bbsreplies_threadid_idx` (`threadid`),
  CONSTRAINT `FKbbsreplies_threadid` FOREIGN KEY (`threadid`) REFERENCES `bbs_threads` (`threadid`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `bbs_threads` (
  `threadid` int unsigned NOT NULL AUTO_INCREMENT,
  `postercid` int unsigned NOT NULL,
  `name` varchar(26) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `timestamp` bigint unsigned NOT NULL,
  `icon` smallint unsigned NOT NULL,
  `replycount` smallint unsigned NOT NULL DEFAULT '0',
  `startpost` text CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `guildid` int unsigned NOT NULL,
  `localthreadid` int unsigned NOT NULL,
  PRIMARY KEY (`threadid`),
  KEY `bbsthreads_guildid_idx` (`guildid`),
  CONSTRAINT `FKbbsthreads_guildid` FOREIGN KEY (`guildid`) REFERENCES `guilds` (`guildid`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `buddies` (
  `id` int NOT NULL AUTO_INCREMENT,
  `characterid` int(11) unsigned zerofill NOT NULL,
  `buddyid` int(11) unsigned zerofill NOT NULL,
  `pending` tinyint NOT NULL DEFAULT '0',
  `group` varchar(17) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `buddies_buddyid_idx` (`buddyid`) /*!80000 INVISIBLE */,
  KEY `buddies_characterid_idx` (`characterid`) /*!80000 INVISIBLE */,
  CONSTRAINT `FKbuddies_buddy` FOREIGN KEY (`buddyid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `FKbuddies_charid` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=14589 DEFAULT CHARSET=utf8;

CREATE TABLE `characters` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `accountid` int NOT NULL DEFAULT '0',
  `world` int NOT NULL DEFAULT '0',
  `name` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `level` int NOT NULL DEFAULT '1',
  `exp` int NOT NULL DEFAULT '0',
  `gachaexp` int NOT NULL DEFAULT '0',
  `str` int NOT NULL DEFAULT '12',
  `dex` int NOT NULL DEFAULT '5',
  `luk` int NOT NULL DEFAULT '4',
  `int` int NOT NULL DEFAULT '4',
  `hp` int NOT NULL DEFAULT '50',
  `mp` int NOT NULL DEFAULT '5',
  `maxhp` int NOT NULL DEFAULT '50',
  `maxmp` int NOT NULL DEFAULT '5',
  `meso` int NOT NULL DEFAULT '0',
  `hpMpUsed` int unsigned NOT NULL DEFAULT '0',
  `job` int NOT NULL DEFAULT '0',
  `skincolor` int NOT NULL DEFAULT '0',
  `gender` int NOT NULL DEFAULT '0',
  `fame` int NOT NULL DEFAULT '0',
  `hair` int NOT NULL DEFAULT '0',
  `face` int NOT NULL DEFAULT '0',
  `ap` int NOT NULL DEFAULT '0',
  `sp` varchar(128) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '0,0,0,0,0,0,0,0,0,0',
  `map` int NOT NULL DEFAULT '0',
  `spawnpoint` int NOT NULL DEFAULT '0',
  `gm` tinyint(1) NOT NULL DEFAULT '0',
  `party` int NOT NULL DEFAULT '0',
  `buddyCapacity` int NOT NULL DEFAULT '25',
  `createdate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `rank` int unsigned NOT NULL DEFAULT '1',
  `rankMove` int NOT NULL DEFAULT '0',
  `jobRank` int unsigned NOT NULL DEFAULT '1',
  `jobRankMove` int NOT NULL DEFAULT '0',
  `guildid` int unsigned NOT NULL DEFAULT '0',
  `guildrank` int unsigned NOT NULL DEFAULT '5',
  `messengerid` int unsigned NOT NULL DEFAULT '0',
  `messengerposition` int unsigned NOT NULL DEFAULT '4',
  `mountlevel` int NOT NULL DEFAULT '1',
  `mountexp` int NOT NULL DEFAULT '0',
  `mounttiredness` int NOT NULL DEFAULT '0',
  `omokwins` int NOT NULL DEFAULT '0',
  `omoklosses` int NOT NULL DEFAULT '0',
  `omokties` int NOT NULL DEFAULT '0',
  `matchcardwins` int NOT NULL DEFAULT '0',
  `matchcardlosses` int NOT NULL DEFAULT '0',
  `matchcardties` int NOT NULL DEFAULT '0',
  `MerchantMesos` int DEFAULT '0',
  `HasMerchant` tinyint(1) DEFAULT '0',
  `equipslots` int NOT NULL DEFAULT '24',
  `useslots` int NOT NULL DEFAULT '24',
  `setupslots` int NOT NULL DEFAULT '24',
  `etcslots` int NOT NULL DEFAULT '24',
  `familyId` int NOT NULL DEFAULT '-1',
  `monsterbookcover` int NOT NULL DEFAULT '0',
  `allianceRank` int NOT NULL DEFAULT '5',
  `vanquisherStage` int unsigned NOT NULL DEFAULT '0',
  `dojoPoints` int unsigned NOT NULL DEFAULT '0',
  `lastDojoStage` int unsigned NOT NULL DEFAULT '0',
  `finishedDojoTutorial` tinyint unsigned NOT NULL DEFAULT '0',
  `vanquisherKills` int unsigned NOT NULL DEFAULT '0',
  `summonValue` int unsigned NOT NULL DEFAULT '0',
  `partnerId` int NOT NULL DEFAULT '0',
  `reborns` int NOT NULL DEFAULT '0',
  `PQPoints` int NOT NULL DEFAULT '0',
  `dataString` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `lastLogoutTime` timestamp NOT NULL DEFAULT '2015-01-01 06:00:00',
  `pendantExp` tinyint(1) NOT NULL DEFAULT '0',
  `fishingpoints` int NOT NULL DEFAULT '0',
  `eventpoints` int NOT NULL DEFAULT '0',
  `rebirthpoints` int NOT NULL DEFAULT '0',
  `jumpquestpoints` int NOT NULL DEFAULT '0',
  `daily` timestamp NOT NULL DEFAULT '1990-01-01 09:00:00',
  `occupation` tinyint(1) NOT NULL DEFAULT '-1',
  `occupation_level` int NOT NULL DEFAULT '0',
  `level_reward` tinyint unsigned NOT NULL DEFAULT '0',
  `chattype` tinyint unsigned NOT NULL DEFAULT '0',
  `msi_creations` int unsigned NOT NULL DEFAULT '0',
  `party_quest_points` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `characters_accountid_idx` (`accountid`),
  KEY `characters_partyid_idx` (`party`),
  KEY `characters_level_exp_ranking` (`level`,`exp`),
  KEY `characters_job_gm_ranking` (`gm`,`job`),
  KEY `characters_worldid_idx` (`world`),
  CONSTRAINT `FKaccounts_characters` FOREIGN KEY (`accountid`) REFERENCES `accounts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=149 DEFAULT CHARSET=utf8 PACK_KEYS=0;

CREATE TABLE `cooldowns` (
  `player_id` int unsigned NOT NULL,
  `skill_id` int NOT NULL,
  `duration` bigint unsigned NOT NULL,
  `created_at` bigint unsigned NOT NULL,
  `type` tinyint unsigned NOT NULL,
  KEY `cooldowns_charid_idx` (`player_id`),
  CONSTRAINT `FKcooldowns_charid` FOREIGN KEY (`player_id`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `cquest` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `questid` int unsigned NOT NULL,
  `characterid` int unsigned NOT NULL,
  `completed` tinyint unsigned NOT NULL,
  `completion` bigint NOT NULL DEFAULT '-1',
  PRIMARY KEY (`id`),
  KEY `Index_2` (`characterid`),
  CONSTRAINT `FKcharacters_cquest` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=37374 DEFAULT CHARSET=utf8;

CREATE TABLE `cquestdata` (
  `qtableid` int unsigned NOT NULL,
  `monsterid` int unsigned NOT NULL,
  `kills` int unsigned NOT NULL DEFAULT '0',
  KEY `cqdata_index` (`qtableid`),
  CONSTRAINT `FKcquest_cquestdata` FOREIGN KEY (`qtableid`) REFERENCES `cquest` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `drop_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dropperid` int NOT NULL,
  `itemid` int NOT NULL DEFAULT '0',
  `minimum_quantity` int NOT NULL DEFAULT '1',
  `maximum_quantity` int NOT NULL DEFAULT '1',
  `questid` int NOT NULL DEFAULT '0',
  `chance` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `mobid` (`dropperid`)
) ENGINE=MyISAM AUTO_INCREMENT=11303 DEFAULT CHARSET=utf8;

CREATE TABLE `drop_data_global` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `continent` int NOT NULL,
  `dropType` tinyint(1) NOT NULL DEFAULT '0',
  `itemid` int NOT NULL DEFAULT '0',
  `minimum_quantity` int NOT NULL DEFAULT '1',
  `maximum_quantity` int NOT NULL DEFAULT '1',
  `questid` int NOT NULL DEFAULT '0',
  `chance` int NOT NULL DEFAULT '0',
  `comments` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `mobid` (`continent`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

CREATE TABLE `dueyitems` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `PackageId` int unsigned NOT NULL DEFAULT '0',
  `itemid` int unsigned NOT NULL DEFAULT '0',
  `quantity` int unsigned NOT NULL DEFAULT '0',
  `upgradeslots` int DEFAULT '0',
  `level` int DEFAULT '0',
  `str` int DEFAULT '0',
  `dex` int DEFAULT '0',
  `int` int DEFAULT '0',
  `luk` int DEFAULT '0',
  `hp` int DEFAULT '0',
  `mp` int DEFAULT '0',
  `watk` int DEFAULT '0',
  `matk` int DEFAULT '0',
  `wdef` int DEFAULT '0',
  `mdef` int DEFAULT '0',
  `acc` int DEFAULT '0',
  `avoid` int DEFAULT '0',
  `hands` int DEFAULT '0',
  `speed` int DEFAULT '0',
  `jump` int DEFAULT '0',
  `owner` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `PackageId` (`PackageId`),
  CONSTRAINT `dueyitems_ibfk_1` FOREIGN KEY (`PackageId`) REFERENCES `dueypackages` (`PackageId`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `dueypackages` (
  `PackageId` int unsigned NOT NULL AUTO_INCREMENT,
  `RecieverId` int unsigned NOT NULL,
  `SenderName` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `Mesos` int unsigned DEFAULT '0',
  `TimeStamp` varchar(10) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `Checked` tinyint unsigned DEFAULT '1',
  `Type` tinyint unsigned NOT NULL,
  PRIMARY KEY (`PackageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `entry_limit` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `playerid` int unsigned NOT NULL,
  `type` varchar(25) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `entries` tinyint unsigned NOT NULL DEFAULT '0',
  `last_entry` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Index_2` (`playerid`,`type`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8;

CREATE TABLE `famelog` (
  `famelogid` int NOT NULL AUTO_INCREMENT,
  `characterid` int unsigned NOT NULL DEFAULT '0',
  `characterid_to` int NOT NULL DEFAULT '0',
  `when` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`famelogid`),
  KEY `characterid` (`characterid`),
  CONSTRAINT `FKcharacter_famelog` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `family_character` (
  `cid` int NOT NULL,
  `familyid` int NOT NULL,
  `rank` int NOT NULL,
  `reputation` int NOT NULL,
  `todaysrep` int NOT NULL,
  `totaljuniors` int NOT NULL,
  `name` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `juniorsadded` int NOT NULL,
  `totalreputation` int NOT NULL,
  PRIMARY KEY (`cid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `gifts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `to` int NOT NULL,
  `from` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `message` tinytext CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `sn` int unsigned NOT NULL,
  `ringid` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

CREATE TABLE `guilds` (
  `guildid` int unsigned NOT NULL AUTO_INCREMENT,
  `leader` int unsigned NOT NULL DEFAULT '0',
  `GP` int unsigned NOT NULL DEFAULT '0',
  `logo` int unsigned DEFAULT NULL,
  `logoColor` smallint unsigned NOT NULL DEFAULT '0',
  `name` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `rank1title` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Master',
  `rank2title` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Jr. Master',
  `rank3title` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `rank4title` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `rank5title` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT 'Member',
  `capacity` int unsigned NOT NULL DEFAULT '10',
  `logoBG` int unsigned DEFAULT NULL,
  `logoBGColor` smallint unsigned NOT NULL DEFAULT '0',
  `notice` varchar(101) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `signature` int NOT NULL DEFAULT '0',
  `allianceId` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`guildid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

CREATE TABLE `hiredmerchant` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ownerid` int DEFAULT '0',
  `itemid` int unsigned NOT NULL DEFAULT '0',
  `quantity` int unsigned NOT NULL DEFAULT '0',
  `upgradeslots` int DEFAULT '0',
  `level` int DEFAULT '0',
  `str` int DEFAULT '0',
  `dex` int DEFAULT '0',
  `int` int DEFAULT '0',
  `luk` int DEFAULT '0',
  `hp` int DEFAULT '0',
  `mp` int DEFAULT '0',
  `watk` int DEFAULT '0',
  `matk` int DEFAULT '0',
  `wdef` int DEFAULT '0',
  `mdef` int DEFAULT '0',
  `acc` int DEFAULT '0',
  `avoid` int DEFAULT '0',
  `hands` int DEFAULT '0',
  `speed` int DEFAULT '0',
  `jump` int DEFAULT '0',
  `owner` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT '',
  `type` tinyint unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `houses` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ownerID` int unsigned NOT NULL,
  `mapID` int unsigned NOT NULL,
  `password` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `purchased` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `bill` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `Index_2` (`ownerID`,`mapID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

CREATE TABLE `htsquads` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `channel` int unsigned NOT NULL,
  `leaderid` int unsigned NOT NULL DEFAULT '0',
  `status` int unsigned NOT NULL DEFAULT '0',
  `members` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `hwidbans` (
  `hwid` varchar(30) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  KEY `Index_1` (`hwid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ign_reserves` (
  `username` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `reserve` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  KEY `Index_1` (`username`),
  KEY `Index_2` (`reserve`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `inventoryequipment` (
  `inventoryitemid` int unsigned NOT NULL DEFAULT '0',
  `upgradeslots` int NOT NULL DEFAULT '0',
  `level` int NOT NULL DEFAULT '0',
  `str` int NOT NULL DEFAULT '0',
  `dex` int NOT NULL DEFAULT '0',
  `int` int NOT NULL DEFAULT '0',
  `luk` int NOT NULL DEFAULT '0',
  `hp` int NOT NULL DEFAULT '0',
  `mp` int NOT NULL DEFAULT '0',
  `watk` int NOT NULL DEFAULT '0',
  `matk` int NOT NULL DEFAULT '0',
  `wdef` int NOT NULL DEFAULT '0',
  `mdef` int NOT NULL DEFAULT '0',
  `acc` int NOT NULL DEFAULT '0',
  `avoid` int NOT NULL DEFAULT '0',
  `hands` int NOT NULL DEFAULT '0',
  `speed` int NOT NULL DEFAULT '0',
  `jump` int NOT NULL DEFAULT '0',
  `locked` int NOT NULL DEFAULT '0',
  `vicious` int unsigned NOT NULL DEFAULT '0',
  `itemlevel` int NOT NULL DEFAULT '1',
  `itemexp` int unsigned NOT NULL DEFAULT '0',
  `ringid` int NOT NULL DEFAULT '-1',
  `eliminations` int unsigned NOT NULL DEFAULT '0',
  `regalia` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`inventoryitemid`),
  KEY `inv_equip_ringid_idx` (`ringid`),
  KEY `equipitemid_idx` (`inventoryitemid`),
  CONSTRAINT `FKinvitems_invequips` FOREIGN KEY (`inventoryitemid`) REFERENCES `inventoryitems` (`inventoryitemid`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `inventoryitems` (
  `inventoryitemid` int unsigned NOT NULL AUTO_INCREMENT,
  `type` tinyint unsigned NOT NULL,
  `characterid` int unsigned DEFAULT NULL,
  `accountid` int unsigned DEFAULT NULL,
  `itemid` int NOT NULL DEFAULT '0',
  `inventorytype` int NOT NULL DEFAULT '0',
  `position` int NOT NULL DEFAULT '0',
  `quantity` int NOT NULL DEFAULT '0',
  `owner` tinytext CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `petid` int NOT NULL DEFAULT '-1',
  `flag` int NOT NULL,
  `expiration` bigint NOT NULL DEFAULT '-1',
  `giftFrom` varchar(26) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`inventoryitemid`),
  KEY `inventoryitems_charid_idx` (`characterid`),
  KEY `inventoryitemid_idx` (`inventoryitemid`),
  KEY `inventory_charid_type_idx` (`type`,`characterid`) /*!80000 INVISIBLE */,
  KEY `inventory_petid_idx` (`petid`),
  CONSTRAINT `FKcharacters_invitems` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=845608 DEFAULT CHARSET=utf8;

CREATE TABLE `ipbans` (
  `ip` varchar(40) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  KEY `Index_1` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `iplog` (
  `iplogid` int unsigned NOT NULL AUTO_INCREMENT,
  `accountid` int NOT NULL DEFAULT '0',
  `ip` varchar(30) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `login` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`iplogid`),
  KEY `accountid` (`accountid`,`ip`),
  KEY `ip` (`ip`)
) ENGINE=InnoDB AUTO_INCREMENT=755 DEFAULT CHARSET=utf8;

CREATE TABLE `jails` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `playerid` int unsigned NOT NULL,
  `reason` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `accuser` int unsigned NOT NULL,
  `when` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `Index_2` (`playerid`,`accuser`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8;

CREATE TABLE `keymap` (
  `characterid` int unsigned NOT NULL DEFAULT '0',
  `key` int NOT NULL DEFAULT '0',
  `type` int NOT NULL DEFAULT '0',
  `action` int NOT NULL DEFAULT '0',
  KEY `Index_1` (`characterid`),
  CONSTRAINT `FKkeymap_charid` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `loggers` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `author` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `description` varchar(400) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=76 DEFAULT CHARSET=utf8;

CREATE TABLE `macbans` (
  `mac` varchar(30) NOT NULL,
  KEY `Index_1` (`mac`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `macfilters` (
  `macfilterid` int unsigned NOT NULL AUTO_INCREMENT,
  `filter` varchar(30) NOT NULL,
  PRIMARY KEY (`macfilterid`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

CREATE TABLE `makercreatedata` (
  `id` tinyint unsigned NOT NULL,
  `itemid` int NOT NULL,
  `req_level` tinyint unsigned NOT NULL,
  `req_maker_level` tinyint unsigned NOT NULL,
  `req_meso` int NOT NULL,
  `req_item` int NOT NULL,
  `req_equip` int NOT NULL,
  `catalyst` int NOT NULL,
  `quantity` smallint NOT NULL,
  `tuc` tinyint NOT NULL,
  PRIMARY KEY (`id`,`itemid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `makerrecipedata` (
  `itemid` int NOT NULL,
  `req_item` int NOT NULL,
  `count` smallint NOT NULL,
  PRIMARY KEY (`itemid`,`req_item`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `makerrewarddata` (
  `itemid` int NOT NULL,
  `rewardid` int NOT NULL,
  `quantity` smallint NOT NULL,
  `prob` tinyint unsigned NOT NULL DEFAULT '100',
  PRIMARY KEY (`itemid`,`rewardid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `marriages` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `groom` int unsigned NOT NULL,
  `bride` int unsigned NOT NULL,
  `engagementbox` int unsigned NOT NULL,
  `married` tinyint unsigned NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `FKmarriage_groom_idx` (`groom`),
  KEY `FKmarriage_bride_idx` (`bride`),
  CONSTRAINT `FKmarriage_bride` FOREIGN KEY (`bride`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `FKmarriage_groom` FOREIGN KEY (`groom`) REFERENCES `characters` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8;

CREATE TABLE `medalmaps` (
  `id` int NOT NULL AUTO_INCREMENT,
  `queststatusid` int unsigned NOT NULL,
  `mapid` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `queststatusid` (`queststatusid`)
) ENGINE=InnoDB AUTO_INCREMENT=61599 DEFAULT CHARSET=utf8;

CREATE TABLE `monsterbook` (
  `charid` int unsigned NOT NULL,
  `cardid` int NOT NULL,
  `level` int DEFAULT '1'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `monstercarddata` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cardid` int NOT NULL DEFAULT '0',
  `mobid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `mts_cart` (
  `id` int NOT NULL AUTO_INCREMENT,
  `cid` int NOT NULL,
  `itemid` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `mts_items` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `tab` int NOT NULL DEFAULT '0',
  `type` int NOT NULL DEFAULT '0',
  `itemid` int unsigned NOT NULL DEFAULT '0',
  `quantity` int NOT NULL DEFAULT '1',
  `seller` int NOT NULL DEFAULT '0',
  `price` int NOT NULL DEFAULT '0',
  `bid_incre` int DEFAULT '0',
  `buy_now` int DEFAULT '0',
  `position` int DEFAULT '0',
  `upgradeslots` int DEFAULT '0',
  `level` int DEFAULT '0',
  `str` int DEFAULT '0',
  `dex` int DEFAULT '0',
  `int` int DEFAULT '0',
  `luk` int DEFAULT '0',
  `hp` int DEFAULT '0',
  `mp` int DEFAULT '0',
  `watk` int DEFAULT '0',
  `matk` int DEFAULT '0',
  `wdef` int DEFAULT '0',
  `mdef` int DEFAULT '0',
  `acc` int DEFAULT '0',
  `avoid` int DEFAULT '0',
  `hands` int DEFAULT '0',
  `speed` int DEFAULT '0',
  `jump` int DEFAULT '0',
  `locked` int DEFAULT '0',
  `isequip` int DEFAULT '0',
  `owner` varchar(16) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT '',
  `sellername` varchar(16) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `sell_ends` varchar(16) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `transfer` int DEFAULT '0',
  `vicious` int unsigned NOT NULL DEFAULT '0',
  `flag` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `notes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `to` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `from` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT '',
  `message` text CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `timestamp` bigint unsigned NOT NULL,
  `fame` int NOT NULL DEFAULT '0',
  `deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8;

CREATE TABLE `nxcode` (
  `code` varchar(15) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `valid` int NOT NULL DEFAULT '1',
  `user` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `type` int NOT NULL DEFAULT '0',
  `item` int NOT NULL DEFAULT '10000',
  PRIMARY KEY (`code`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `pets` (
  `petid` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `level` int unsigned NOT NULL,
  `closeness` int unsigned NOT NULL,
  `fullness` int unsigned NOT NULL,
  `summoned` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`petid`)
) ENGINE=InnoDB AUTO_INCREMENT=172 DEFAULT CHARSET=utf8;

CREATE TABLE `playernpcs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `hair` int NOT NULL,
  `face` int NOT NULL,
  `skin` int NOT NULL,
  `x` int NOT NULL,
  `cy` int NOT NULL DEFAULT '0',
  `map` int NOT NULL,
  `gender` int NOT NULL DEFAULT '0',
  `dir` int NOT NULL DEFAULT '0',
  `scriptid` int unsigned NOT NULL DEFAULT '0',
  `foothold` int NOT NULL DEFAULT '0',
  `rx0` int NOT NULL DEFAULT '0',
  `rx1` int NOT NULL DEFAULT '0',
  `script` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8;

CREATE TABLE `playernpcs_equip` (
  `id` int NOT NULL AUTO_INCREMENT,
  `npcid` int NOT NULL DEFAULT '0',
  `equipid` int NOT NULL,
  `type` int NOT NULL DEFAULT '0',
  `equippos` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8;

CREATE TABLE `questactions` (
  `questactionid` int unsigned NOT NULL AUTO_INCREMENT,
  `questid` int NOT NULL DEFAULT '0',
  `status` int NOT NULL DEFAULT '0',
  `data` blob NOT NULL,
  PRIMARY KEY (`questactionid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `questprogress` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `queststatusid` int unsigned NOT NULL DEFAULT '0',
  `progressid` int NOT NULL DEFAULT '0',
  `progress` varchar(15) CHARACTER SET latin1 COLLATE latin1_german1_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3481 DEFAULT CHARSET=utf8;

CREATE TABLE `questrequirements` (
  `questrequirementid` int unsigned NOT NULL AUTO_INCREMENT,
  `questid` int NOT NULL DEFAULT '0',
  `status` int NOT NULL DEFAULT '0',
  `data` blob NOT NULL,
  PRIMARY KEY (`questrequirementid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

CREATE TABLE `queststatus` (
  `queststatusid` int unsigned NOT NULL AUTO_INCREMENT,
  `characterid` int NOT NULL DEFAULT '0',
  `quest` int NOT NULL DEFAULT '0',
  `status` int NOT NULL DEFAULT '0',
  `time` int NOT NULL DEFAULT '0',
  `forfeited` int NOT NULL DEFAULT '0',
  `info` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`queststatusid`)
) ENGINE=InnoDB AUTO_INCREMENT=32211 DEFAULT CHARSET=utf8;

CREATE TABLE `reactordrops` (
  `reactordropid` int unsigned NOT NULL AUTO_INCREMENT,
  `reactorid` int NOT NULL,
  `itemid` int NOT NULL,
  `chance` int NOT NULL,
  `questid` int NOT NULL DEFAULT '-1',
  PRIMARY KEY (`reactordropid`),
  KEY `reactorid` (`reactorid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 PACK_KEYS=1;

CREATE TABLE `reports` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `reporttime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `reporterid` int NOT NULL,
  `victimid` int NOT NULL,
  `reason` tinyint NOT NULL,
  `chatlog` text NOT NULL,
  `status` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `responses` (
  `chat` text CHARACTER SET latin1 COLLATE latin1_swedish_ci,
  `response` text CHARACTER SET latin1 COLLATE latin1_swedish_ci,
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `rings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `partnerRingId` int NOT NULL DEFAULT '0',
  `partnerChrId` int NOT NULL DEFAULT '0',
  `itemid` int NOT NULL DEFAULT '0',
  `partnername` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8;

CREATE TABLE `savedlocations` (
  `characterid` int NOT NULL,
  `locationtype` enum('FREE_MARKET','EVENT','WORLDTOUR','FLORINA','INTRO','SUNDAY_MARKET','MIRROR','DOJO','OTHER') CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `map` int NOT NULL,
  `portal` int NOT NULL,
  KEY `Index_1` (`characterid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `server_queue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `accountid` int NOT NULL DEFAULT '0',
  `characterid` int NOT NULL DEFAULT '0',
  `type` tinyint NOT NULL DEFAULT '0',
  `value` int NOT NULL DEFAULT '0',
  `message` varchar(128) NOT NULL,
  `createTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shopitems` (
  `shopid` int unsigned NOT NULL,
  `itemid` int NOT NULL,
  `price` int NOT NULL,
  `pitch` int NOT NULL DEFAULT '0',
  `position` int NOT NULL COMMENT 'sort is an arbitrary field designed to give leeway when modifying shops. The lowest number is 104 and it increments by 4 for each item to allow decent space for swapping/inserting/removing items.',
  KEY `shopitems_shopid_idx` (`shopid`),
  CONSTRAINT `FKshopitems_shopid` FOREIGN KEY (`shopid`) REFERENCES `shops` (`shopid`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `shops` (
  `shopid` int unsigned NOT NULL AUTO_INCREMENT,
  `npcid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`shopid`),
  KEY `shops_npcid_idx` (`npcid`)
) ENGINE=InnoDB AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8;

CREATE TABLE `skillmacros` (
  `characterid` int unsigned NOT NULL DEFAULT '0',
  `position` tinyint(1) NOT NULL DEFAULT '0',
  `skill1` int NOT NULL DEFAULT '0',
  `skill2` int NOT NULL DEFAULT '0',
  `skill3` int NOT NULL DEFAULT '0',
  `name` varchar(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `shout` tinyint(1) NOT NULL DEFAULT '0',
  KEY `skillmacros_characterid_idx` (`characterid`),
  CONSTRAINT `FKskillmacros_charid` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `skills` (
  `skillid` int NOT NULL DEFAULT '0',
  `characterid` int unsigned NOT NULL DEFAULT '0',
  `skilllevel` int NOT NULL DEFAULT '0',
  `masterlevel` int NOT NULL DEFAULT '0',
  `expiration` bigint NOT NULL DEFAULT '-1',
  KEY `character_id` (`characterid`),
  CONSTRAINT `FKcharid_skills` FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `spawns` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `idd` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `f` int unsigned NOT NULL,
  `fh` int unsigned NOT NULL,
  `x` int NOT NULL,
  `cy` int NOT NULL,
  `type` varchar(1) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `mid` int unsigned NOT NULL,
  `mobtime` int unsigned NOT NULL,
  `script` varchar(45) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `rx0` int NOT NULL,
  `rx1` int NOT NULL,
  `hidden` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=212 DEFAULT CHARSET=utf8;

CREATE TABLE `specialcashitems` (
  `id` int NOT NULL,
  `sn` int NOT NULL,
  `modifier` int NOT NULL COMMENT '1024 is add/remove',
  `info` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `storages` (
  `storageid` int unsigned NOT NULL AUTO_INCREMENT,
  `accountid` int NOT NULL DEFAULT '0',
  `world` int NOT NULL,
  `slots` int NOT NULL DEFAULT '0',
  `meso` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`storageid`),
  KEY `loadable_storage` (`accountid`,`world`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8;

CREATE TABLE `transactions` (
  `ids` int unsigned NOT NULL AUTO_INCREMENT,
  `player_id` int unsigned NOT NULL,
  `trade_log` varchar(400) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ids`),
  KEY `Index_2` (`player_id`)
) ENGINE=InnoDB AUTO_INCREMENT=935 DEFAULT CHARSET=utf8;

CREATE TABLE `trocklocations` (
  `characterid` int NOT NULL,
  `mapid` int NOT NULL,
  `vip` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `unique_giveaways` (
  `mac` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `hdd` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
  `description` varchar(50) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `wishlists` (
  `id` int NOT NULL AUTO_INCREMENT,
  `charid` int NOT NULL,
  `sn` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1619 DEFAULT CHARSET=utf8;

CREATE TABLE `zaksquads` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `channel` int unsigned NOT NULL,
  `leaderid` int unsigned NOT NULL DEFAULT '0',
  `status` int unsigned NOT NULL DEFAULT '0',
  `members` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
