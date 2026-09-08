-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 08, 2026 at 11:04 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `college_attendance`
--

-- --------------------------------------------------------

--
-- Table structure for table `attendance`
--

CREATE TABLE `attendance` (
  `id` bigint(20) NOT NULL,
  `attendance_date` date NOT NULL,
  `check_in_time` time DEFAULT NULL,
  `check_out_time` time DEFAULT NULL,
  `confidence` double DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('ABSENT','PRESENT') NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `check_in_latitude` double DEFAULT NULL,
  `check_in_longitude` double DEFAULT NULL,
  `check_out_latitude` double DEFAULT NULL,
  `check_out_longitude` double DEFAULT NULL,
  `source` enum('MACHINE','MOBILE') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `attendance`
--

INSERT INTO `attendance` (`id`, `attendance_date`, `check_in_time`, `check_out_time`, `confidence`, `created_at`, `status`, `user_id`, `check_in_latitude`, `check_in_longitude`, `check_out_latitude`, `check_out_longitude`, `source`) VALUES
(1, '2026-08-25', '09:05:00', NULL, 0.94, '2026-08-25 14:40:30.000000', 'PRESENT', 8, NULL, NULL, NULL, NULL, 'MACHINE'),
(2, '2026-09-01', '09:37:02', '15:38:35', 0.8259, '2026-09-01 09:37:02.000000', 'PRESENT', 8, NULL, NULL, NULL, NULL, 'MACHINE'),
(3, '2026-09-02', '14:08:28', NULL, 43.33333333333332, '2026-09-02 14:08:28.000000', 'PRESENT', 8, NULL, NULL, NULL, NULL, 'MACHINE'),
(4, '2026-09-03', '14:15:16', NULL, NULL, '2026-09-03 14:15:16.000000', 'PRESENT', 8, NULL, NULL, NULL, NULL, 'MACHINE'),
(5, '2026-09-04', '15:49:39', '17:10:28', 74.1, '2026-09-04 15:49:39.000000', 'PRESENT', 8, NULL, NULL, 28.1228822, 77.5542396, 'MOBILE'),
(6, '2026-09-07', '14:55:00', NULL, 86.58, '2026-09-07 14:55:00.000000', 'PRESENT', 8, NULL, NULL, NULL, NULL, 'MACHINE');

-- --------------------------------------------------------

--
-- Table structure for table `college_attendance_policy`
--

CREATE TABLE `college_attendance_policy` (
  `id` bigint(20) NOT NULL,
  `allowed_radius_meters` decimal(10,2) NOT NULL,
  `attendance_required` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `latitude` decimal(10,7) NOT NULL,
  `location_name` varchar(150) NOT NULL,
  `longitude` decimal(10,7) NOT NULL,
  `policy_name` varchar(100) NOT NULL,
  `saturday_off` bit(1) NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `sunday_off` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `working_hours` decimal(5,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `college_attendance_policy`
--

INSERT INTO `college_attendance_policy` (`id`, `allowed_radius_meters`, `attendance_required`, `created_at`, `latitude`, `location_name`, `longitude`, `policy_name`, `saturday_off`, `status`, `sunday_off`, `updated_at`, `working_hours`) VALUES
(1, 200.00, b'1', '2026-09-07 10:46:42.000000', 28.1222106, 'College Campus', 77.5538813, 'College Attendance policy', b'1', 'ACTIVE', b'1', '2026-09-07 10:46:42.000000', 6.00);

-- --------------------------------------------------------

--
-- Table structure for table `courses`
--

CREATE TABLE `courses` (
  `id` bigint(20) NOT NULL,
  `code` varchar(30) NOT NULL,
  `duration_years` int(11) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `department_id` bigint(20) NOT NULL,
  `status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `courses`
--

INSERT INTO `courses` (`id`, `code`, `duration_years`, `name`, `department_id`, `status`) VALUES
(3, 'MCA', 2, 'Master of Computer Applications', 1, 'ACTIVE'),
(4, 'BCA', NULL, 'Bachelor of Computer Applications', 1, 'ACTIVE'),
(5, 'BTech', NULL, 'Bachelor of Technology', 1, 'ACTIVE'),
(6, 'MBA', NULL, 'Master of Business Administration', 4, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `departments`
--

CREATE TABLE `departments` (
  `id` bigint(20) NOT NULL,
  `code` varchar(30) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `departments`
--

INSERT INTO `departments` (`id`, `code`, `description`, `name`, `status`) VALUES
(1, 'CSE', 'Department of Computer Applications', 'Computer Science', 'ACTIVE'),
(2, 'ECE', NULL, 'Electronics & Communication', 'ACTIVE'),
(3, 'ME', NULL, 'Mechanical Engineering', 'ACTIVE'),
(4, 'MBA', NULL, 'Management', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `face_embeddings`
--

CREATE TABLE `face_embeddings` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `embedding` longtext NOT NULL,
  `model_name` varchar(255) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL,
  `detector_backend` varchar(255) DEFAULT NULL,
  `embedding_size` int(11) NOT NULL,
  `registered_at` datetime(6) NOT NULL,
  `status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `face_embeddings`
--

INSERT INTO `face_embeddings` (`id`, `created_at`, `embedding`, `model_name`, `updated_at`, `user_id`, `detector_backend`, `embedding_size`, `registered_at`, `status`) VALUES
(1, '2026-08-25 12:26:16.000000', '[0.7527597546577454,1.6079750061035156,0.5592026710510254,1.2072252035140991,0.917542040348053,-0.354520708322525,0.012961320579051971,0.9735903143882751,1.3620928525924683,-0.4938032031059265,1.5145437717437744,-0.05996028333902359,0.6262087225914001,-0.6594555974006653,-0.10142409056425095,1.0144122838974,-0.5051403641700745,-0.3951004147529602,-0.31352028250694275,0.161903515458107,0.7133221626281738,1.0208697319030762,-0.3062097132205963,-0.2565259635448456,-0.03809270262718201,-0.391832172870636,0.5755041837692261,-1.6231974363327026,-1.2325325012207031,-0.36920297145843506,0.49701184034347534,1.3939321041107178,-0.5494851469993591,0.8784515857696533,1.895350694656372,1.8145917654037476,1.1744859218597412,-0.3791259527206421,-1.220589280128479,0.7469320297241211,0.8493924140930176,-0.05882277339696884,0.4809175729751587,-0.7020036578178406,-1.6983765363693237,-0.1986025720834732,-0.37691521644592285,0.6200565695762634,-0.16087104380130768,-0.5324043035507202,0.31144750118255615,-1.587379813194275,0.18136157095432281,0.9097386002540588,1.3176556825637817,-0.16086675226688385,0.9642711877822876,-0.7140382528305054,0.658292829990387,-0.6777658462524414,1.419509768486023,-0.1996915489435196,1.2070285081863403,-0.139592707157135,0.7891738414764404,-0.13660681247711182,-0.3402298390865326,-0.9364559054374695,2.5680134296417236,-0.22594627737998962,-0.08326724916696548,-0.9958422780036926,-0.9818221926689148,-0.7702619433403015,2.492607593536377,-0.14677399396896362,-1.3347331285476685,0.844031810760498,0.5068723559379578,0.0820889100432396,1.3102326393127441,-0.26894453167915344,-1.7979276180267334,-1.443455696105957,-0.7171986103057861,0.6664156913757324,1.0005154609680176,0.48136475682258606,1.4740707874298096,-0.25789058208465576,-1.3216315507888794,2.0652878284454346,-1.4273335933685303,-0.25429993867874146,-0.5125010013580322,-0.35168346762657166,-0.08536045253276825,-0.40194880962371826,-1.932536244392395,0.22548699378967285,-0.1206272765994072,-0.4754049777984619,-0.6469640731811523,-0.9506747126579285,0.4195626676082611,0.0743216797709465,1.3953005075454712,-1.8341732025146484,0.25269031524658203,1.3175127506256104,-0.34597304463386536,-0.10793901979923248,0.45343413949012756,-0.5480605959892273,-0.14972800016403198,-0.9631590247154236,-0.09378834813833237,0.5827305316925049,-2.336414098739624,0.13769176602363586,-2.236345052719116,-0.2653832733631134,-0.6716610193252563,0.03506740182638168,-1.2345950603485107,-0.6260998845100403,0.09862689673900604,-1.6816056966781616,0.5117673873901367,1.2403415441513062,0.8527958393096924,-1.4206972122192383,1.0169992446899414,-1.678130865097046,1.3687152862548828,2.3016748428344727,0.2790640890598297,0.541443407535553,0.22530139982700348,0.049583807587623596,2.26351261138916,0.27497002482414246,0.1450578272342682,0.7003797292709351,-0.25675302743911743,0.6065883040428162,-0.05227214843034744,1.9534962177276611,0.8179234862327576,0.7455163598060608,0.5847419500350952,-1.207192301750183,-1.3032771348953247,-0.2810664772987366,-0.5866135954856873,0.40483909845352173,-0.7331730723381042,0.9833855032920837,-1.0838218927383423,-0.35555216670036316,-0.755775511264801,0.30974695086479187,-0.19737182557582855,1.0262675285339355,-1.4748163223266602,-1.3110367059707642,-2.1936707496643066,0.7927101254463196,1.6986823081970215,-0.6221979856491089,0.4249071478843689,-1.7020291090011597,0.2531948983669281,2.343128204345703,0.9345212578773499,0.7261919379234314,-1.1544766426086426,-0.14472952485084534,-0.7530033588409424,0.10088871419429779,-2.11594557762146,0.15274563431739807,0.7051756978034973,-0.9618125557899475,-0.1708046942949295,0.9218321442604065,2.37519907951355,1.0876338481903076,-0.29550182819366455,0.21683350205421448,-0.32561612129211426,1.401922583580017,1.291209101676941,0.5782822966575623,-0.011992576532065868,-0.25754469633102417,-0.03724270686507225,-0.913568913936615,2.2466859817504883,-1.122969627380371,0.15170232951641083,-0.5873821377754211,1.5266200304031372,1.2188934087753296,1.7704206705093384,-0.3069964051246643,0.9897167682647705,0.5259557366371155,-0.21227365732192993,-0.1414550244808197,-1.9682612419128418,-0.7639930844306946,0.1625479906797409,0.21413294970989227,-0.949742317199707,0.8101487755775452,1.29456627368927,-0.9231688380241394,-0.03906573727726936,0.5787229537963867,-0.5109538435935974,0.09151798486709595,0.4409532845020294,2.470449447631836,2.366976499557495,0.1245076134800911,-1.2903298139572144,-0.9503121376037598,-0.1700093299150467,0.31664949655532837,0.042753443121910095,0.6793273091316223,0.46212059259414673,-1.3934659957885742,0.3826974034309387,0.39956122636795044,-1.0613611936569214,2.08853816986084,-0.10222840309143066,0.11048847436904907,-0.6822559237480164,0.318039208650589,1.0983067750930786,0.011529156006872654,0.43635156750679016,1.0309616327285767,0.1437673717737198,-2.0449776649475098,2.1364481449127197,-0.1812533140182495,-0.64100581407547,-0.3152385950088501,-0.66178959608078,0.16838175058364868,-0.720609188079834,-1.13345205783844,-1.5716809034347534,0.9646791219711304,-0.14233699440956116,-0.20801123976707458,0.42069917917251587,-0.16416272521018982,0.6405172348022461,-1.6236441135406494,-0.7522699236869812,-1.1427392959594727,-0.4995272159576416,1.3750847578048706,-1.100905179977417,-1.207908272743225,1.4223748445510864,-0.5482734441757202,-0.8670777082443237,0.1390751451253891,-0.21826906502246857,1.0013470649719238,-0.6341263055801392,-1.584942102432251,-0.7398262023925781,1.8513553142547607,0.22469204664230347,0.7700464725494385,-0.6803076863288879,-0.19838914275169373,-1.3406164646148682,0.26018026471138,-0.9338424205780029,-0.9249604344367981,0.08541239798069,2.9384889602661133,-0.6387341618537903,0.10626640170812607,0.40733328461647034,0.8277437090873718,0.3263471722602844,-0.14798246324062347,0.14330533146858215,-0.9933938384056091,-0.6421650648117065,-0.6967058181762695,-0.45474833250045776,1.6985241174697876,-0.939548909664154,1.0793325901031494,-1.099942922592163,-0.46908044815063477,0.3697323501110077,0.4177219271659851,-0.0918884128332138,-0.11072003096342087,0.3663628101348877,0.02292078547179699,1.1132001876831055,0.34344717860221863,1.2455317974090576,0.6916386485099792,2.538469076156616,0.14191344380378723,0.661367654800415,-2.4488682746887207,-0.5359883904457092,-1.5381304025650024,-0.06597896665334702,0.44863250851631165,0.19367989897727966,1.7321956157684326,0.7992702126502991,1.832472801208496,0.5126621723175049,1.9673503637313843,1.2823212146759033,1.1069371700286865,-0.9499569535255432,-1.6677143573760986,-0.41054046154022217,1.855381727218628,0.3112078309059143,-0.2559620440006256,1.2750141620635986,-0.3781086504459381,0.4337087869644165,-1.394034743309021,-1.4193880558013916,-0.15855593979358673,0.38203275203704834,-0.5097344517707825,-0.7373247146606445,1.4756176471710205,-2.6904916763305664,-1.2057474851608276,-0.9473010301589966,1.3006163835525513,0.44399911165237427,0.1669306457042694,-1.0042142868041992,0.5962573289871216,0.7227705717086792,0.4789819121360779,1.0359846353530884,-0.7970405220985413,-0.010546316392719746,0.7793457508087158,-0.35551291704177856,-0.8889557719230652,0.29328349232673645,2.008143663406372,-1.0858004093170166,1.3316282033920288,-0.16413672268390656,1.3492523431777954,-1.4911595582962036,-0.24358060956001282,-0.4524848461151123,-0.028704717755317688,-1.4667216539382935,0.15393006801605225,1.0940170288085938,-0.2511914074420929,-0.07717697322368622,0.28495877981185913,-0.10301055014133453,0.7382423281669617,1.3820900917053223,-1.1276098489761353,1.1362380981445312,0.5024023652076721,-0.5898650288581848,0.4477218985557556,-0.4006374180316925,0.5667941570281982,0.5717437267303467,-1.3307408094406128,-1.5497950315475464,-0.08331534266471863,-0.9358172416687012,-1.1064865589141846,-0.6222314834594727,0.6577335000038147,-1.1267259120941162,6.236257031559944E-4,-0.562997043132782,-0.8477628827095032,0.6577316522598267,0.4315425753593445,1.2268379926681519,0.9398137927055359,-0.5825639367103577,-0.2732103168964386,1.7134627103805542,1.0592467784881592,1.220857858657837,-0.5908190608024597,0.15547947585582733,-0.813538670539856,-1.7227524518966675,0.22192642092704773,-0.8239275813102722,-1.9946860074996948,-0.21795685589313507,0.823746383190155,0.2966160476207733,-0.1401459276676178,0.2553160786628723,0.153264582157135,0.08479809015989304,1.1423848867416382,-0.053538527339696884,-0.19989986717700958,0.6423336267471313,0.09201018512248993,-0.8505565524101257,-0.32364100217819214,-0.3180488348007202,-1.579839825630188,0.8902307748794556,0.31943055987358093,-1.2314341068267822,-0.8477110862731934,-1.0321770906448364,0.5640116930007935,-0.8298277854919434,-0.677710235118866,0.4000743627548218,0.9064953923225403,1.0737481117248535,-1.2718843221664429,-1.589453935623169,0.6234615445137024,-1.0858441591262817,0.02301667630672455,0.7095956802368164,-2.3604788780212402,-0.8573499321937561,0.7845219373703003,-1.270851492881775,-0.5070491433143616,-0.8948382139205933,0.4242032766342163,0.2385081946849823,0.7224435806274414,0.4310649633407593,1.7502880096435547,1.840342402458191,1.0473519563674927,-3.2652337551116943,0.046961262822151184,1.3538336753845215,-0.7110930681228638,-0.6171152591705322,-1.157149314880371,-0.39159664511680603,-0.923078179359436,-0.02544560842216015,-0.8858921527862549,-0.4296457767486572,0.5329480767250061,-2.6014373302459717,1.2936196327209473,0.03839974105358124,-1.711172103881836,-0.132717564702034,0.5388721227645874,0.535893976688385,-0.3871275782585144,-0.15967874228954315,-1.4262226819992065,1.0835148096084595,0.7094628810882568,-1.8011548519134521,-1.0245712995529175,0.7036734223365784,-2.0587847232818604,0.566862165927887,0.39933502674102783,-0.7879806756973267,0.3953826427459717,0.6574375629425049,1.5457342863082886,0.8616300821304321,-0.6322846412658691,1.5400402545928955,0.12630942463874817,0.8941462635993958,-0.4198160469532013,-1.5188957452774048,-1.6100109815597534,0.3536960780620575,-1.105905294418335,0.7166672945022583,-0.6599462628364563,-1.0191855430603027,1.9671556949615479]', 'Facenet512', NULL, 8, 'retinaface', 512, '2026-08-25 12:26:16.000000', 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `holidays`
--

CREATE TABLE `holidays` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `holiday_date` date NOT NULL,
  `holiday_name` varchar(150) NOT NULL,
  `holiday_type` enum('COLLEGE','FESTIVAL','NATIONAL','OTHER') NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `holidays`
--

INSERT INTO `holidays` (`id`, `created_at`, `description`, `holiday_date`, `holiday_name`, `holiday_type`, `status`, `updated_at`) VALUES
(1, '2026-09-07 10:29:52.000000', 'Birthday of Mahatma Gandi', '2026-10-02', 'Gandi Janti', 'NATIONAL', 'ACTIVE', '2026-09-07 10:29:52.000000');

-- --------------------------------------------------------

--
-- Table structure for table `leaves`
--

CREATE TABLE `leaves` (
  `id` bigint(20) NOT NULL,
  `admin_remark` varchar(500) DEFAULT NULL,
  `applied_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(500) NOT NULL,
  `from_date` date NOT NULL,
  `leave_type` enum('CASUAL','OTHER','PERSONAL','SICK') NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` bigint(20) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') NOT NULL,
  `to_date` date NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `leaves`
--

INSERT INTO `leaves` (`id`, `admin_remark`, `applied_at`, `created_at`, `description`, `from_date`, `leave_type`, `reviewed_at`, `reviewed_by`, `status`, `to_date`, `updated_at`, `user_id`) VALUES
(1, NULL, '2026-09-07 15:44:07.000000', '2026-09-07 15:44:07.000000', 'urgent work', '2026-09-07', 'CASUAL', NULL, NULL, 'PENDING', '2026-09-08', '2026-09-07 15:44:07.000000', 8),
(2, NULL, '2026-09-07 16:40:51.000000', '2026-09-07 16:40:51.000000', 'Birthday party', '2026-09-18', 'PERSONAL', '2026-09-07 16:41:03.000000', 1, 'APPROVED', '2026-09-18', '2026-09-07 16:41:03.000000', 8);

-- --------------------------------------------------------

--
-- Table structure for table `sections`
--

CREATE TABLE `sections` (
  `id` bigint(20) NOT NULL,
  `name` varchar(50) NOT NULL,
  `semester_id` bigint(20) NOT NULL,
  `status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `sections`
--

INSERT INTO `sections` (`id`, `name`, `semester_id`, `status`) VALUES
(2, 'A', 2, 'ACTIVE'),
(3, 'B', 3, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `semesters`
--

CREATE TABLE `semesters` (
  `id` bigint(20) NOT NULL,
  `semester_number` int(11) NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `status` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `semesters`
--

INSERT INTO `semesters` (`id`, `semester_number`, `course_id`, `status`) VALUES
(2, 1, 3, 'ACTIVE'),
(3, 2, 3, 'ACTIVE'),
(4, 3, 3, 'ACTIVE'),
(5, 4, 3, 'ACTIVE'),
(6, 1, 4, 'ACTIVE'),
(7, 2, 4, 'ACTIVE'),
(8, 3, 4, 'ACTIVE'),
(9, 4, 4, 'ACTIVE'),
(10, 5, 4, 'ACTIVE'),
(11, 6, 4, 'ACTIVE');

-- --------------------------------------------------------

--
-- Table structure for table `students`
--

CREATE TABLE `students` (
  `id` bigint(20) NOT NULL,
  `email` varchar(150) DEFAULT NULL,
  `name` varchar(150) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `roll_number` varchar(50) NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `section_id` bigint(20) NOT NULL,
  `semester_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `students`
--

INSERT INTO `students` (`id`, `email`, `name`, `phone`, `roll_number`, `course_id`, `section_id`, `semester_id`, `user_id`) VALUES
(1, 'test@example.com', 'Manish', '7505988428', 'MCA001', 3, 2, 2, 8),
(2, 'aman@example.com', 'Aman', '7687899210', 'MCA002', 3, 2, 2, 9);

-- --------------------------------------------------------

--
-- Table structure for table `student_face_profiles`
--

CREATE TABLE `student_face_profiles` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `embedding` longtext NOT NULL,
  `embedding_size` int(11) NOT NULL,
  `model` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `student_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `teachers`
--

CREATE TABLE `teachers` (
  `id` bigint(20) NOT NULL,
  `email` varchar(150) DEFAULT NULL,
  `employee_code` varchar(50) NOT NULL,
  `name` varchar(150) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `department_id` bigint(20) DEFAULT NULL,
  `user_id` bigint(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `teachers`
--

INSERT INTO `teachers` (`id`, `email`, `employee_code`, `name`, `phone`, `department_id`, `user_id`) VALUES
(1, 'raghav@gmail.com', 'Emp001', 'Raghav', '9876543342', 1, 10);

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','STUDENT','TEACHER') NOT NULL,
  `status` enum('ACTIVE','INACTIVE') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `username` varchar(100) NOT NULL,
  `profile_image_url` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `created_at`, `password`, `role`, `status`, `updated_at`, `username`, `profile_image_url`) VALUES
(1, '2026-08-24 12:40:56.000000', '$2a$10$XpIzxkHil8HkvAIudqo9a.PDb/.EeBMbvrsJDZQ9ifTSdPeLBk1.6', 'ADMIN', 'ACTIVE', '2026-08-24 12:40:56.000000', 'admin', NULL),
(8, '2026-08-25 12:46:21.000000', '$2a$10$gHByIu7K5eDTomwIH.9Rae97iFFryxo7nWumf/4bmQH038Nw9u4kq', 'STUDENT', 'ACTIVE', '2026-09-08 11:43:29.000000', 'MCA001', '/uploads/profiles/8.jpg'),
(9, '2026-08-25 12:53:06.000000', '$2a$10$nclRWFK4BFf9.E2iEpdmtO07cHBuSii51XFgszzINCGr3VyEZ.XPi', 'STUDENT', 'ACTIVE', '2026-09-08 11:22:24.000000', 'MCA002', '/uploads/profiles/9.jpg'),
(10, '2026-09-01 12:28:37.000000', '$2a$10$DVFpW48FhAe8GQSVIfQVcOFPeu.5Q0isgeRxbQwOEQKZddsZ8iIlO', 'TEACHER', 'ACTIVE', '2026-09-08 11:48:14.000000', 'Emp001', '/uploads/profiles/10.jpg');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `attendance`
--
ALTER TABLE `attendance`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_attendance_user_date` (`user_id`,`attendance_date`),
  ADD KEY `idx_attendance_date` (`attendance_date`),
  ADD KEY `idx_attendance_user` (`user_id`);

--
-- Indexes for table `college_attendance_policy`
--
ALTER TABLE `college_attendance_policy`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `courses`
--
ALTER TABLE `courses`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_course_code` (`code`),
  ADD KEY `fk_course_department` (`department_id`);

--
-- Indexes for table `departments`
--
ALTER TABLE `departments`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_department_code` (`code`);

--
-- Indexes for table `face_embeddings`
--
ALTER TABLE `face_embeddings`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKg2ffm5dntor916lxddoxl5071` (`user_id`),
  ADD KEY `idx_face_embedding_user` (`user_id`),
  ADD KEY `idx_face_user_id` (`user_id`);

--
-- Indexes for table `holidays`
--
ALTER TABLE `holidays`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_holiday_date` (`holiday_date`);

--
-- Indexes for table `leaves`
--
ALTER TABLE `leaves`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `sections`
--
ALTER TABLE `sections`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_section_semester_name` (`semester_id`,`name`);

--
-- Indexes for table `semesters`
--
ALTER TABLE `semesters`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_semester_course_number` (`course_id`,`semester_number`);

--
-- Indexes for table `students`
--
ALTER TABLE `students`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_student_roll_number` (`roll_number`),
  ADD UNIQUE KEY `UKg4fwvutq09fjdlb4bb0byp7t` (`user_id`),
  ADD KEY `fk_student_course` (`course_id`),
  ADD KEY `fk_student_section` (`section_id`),
  ADD KEY `fk_student_semester` (`semester_id`);

--
-- Indexes for table `student_face_profiles`
--
ALTER TABLE `student_face_profiles`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_student_face_student` (`student_id`);

--
-- Indexes for table `teachers`
--
ALTER TABLE `teachers`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_teacher_employee_code` (`employee_code`),
  ADD UNIQUE KEY `UKcd1k6xwg9jqtiwx9ybnxpmoh9` (`user_id`),
  ADD KEY `fk_teacher_department` (`department_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  ADD KEY `idx_user_username` (`username`),
  ADD KEY `idx_user_role` (`role`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `attendance`
--
ALTER TABLE `attendance`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `college_attendance_policy`
--
ALTER TABLE `college_attendance_policy`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `courses`
--
ALTER TABLE `courses`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `departments`
--
ALTER TABLE `departments`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `face_embeddings`
--
ALTER TABLE `face_embeddings`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `holidays`
--
ALTER TABLE `holidays`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `leaves`
--
ALTER TABLE `leaves`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `sections`
--
ALTER TABLE `sections`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `semesters`
--
ALTER TABLE `semesters`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `students`
--
ALTER TABLE `students`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `student_face_profiles`
--
ALTER TABLE `student_face_profiles`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `teachers`
--
ALTER TABLE `teachers`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `attendance`
--
ALTER TABLE `attendance`
  ADD CONSTRAINT `fk_attendance_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `courses`
--
ALTER TABLE `courses`
  ADD CONSTRAINT `fk_course_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`);

--
-- Constraints for table `face_embeddings`
--
ALTER TABLE `face_embeddings`
  ADD CONSTRAINT `fk_face_embedding_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `sections`
--
ALTER TABLE `sections`
  ADD CONSTRAINT `fk_section_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`);

--
-- Constraints for table `semesters`
--
ALTER TABLE `semesters`
  ADD CONSTRAINT `fk_semester_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`);

--
-- Constraints for table `students`
--
ALTER TABLE `students`
  ADD CONSTRAINT `fk_student_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  ADD CONSTRAINT `fk_student_section` FOREIGN KEY (`section_id`) REFERENCES `sections` (`id`),
  ADD CONSTRAINT `fk_student_semester` FOREIGN KEY (`semester_id`) REFERENCES `semesters` (`id`),
  ADD CONSTRAINT `fk_student_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

--
-- Constraints for table `student_face_profiles`
--
ALTER TABLE `student_face_profiles`
  ADD CONSTRAINT `fk_student_face_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`);

--
-- Constraints for table `teachers`
--
ALTER TABLE `teachers`
  ADD CONSTRAINT `fk_teacher_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  ADD CONSTRAINT `fk_teacher_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
