<?php
if(count(get_included_files()) == 1) die("pls don't");

chdir(dirname(__FILE__));

$CONFIG["folder"] = "msgs";
$CONFIG["fileprefix"] = "msg";
$CONFIG["filesuffix"] = ".txt";

$CONFIG["file"] = $CONFIG["fileprefix"]."_%u_%u".$CONFIG["filesuffix"];
$CONFIG["fullfile"] = $CONFIG["folder"]."/".$CONFIG["file"];
?>
