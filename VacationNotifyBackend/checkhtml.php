<?php
require_once("config.php");
require_once("checkhelper.php");
$messages = get_messages();
foreach($messages as $message){
  echo "<p><a href=\"received.php?time=".$message[0]."&code=".$message[1]."\" target=\"_blank\">".$message[0]." ".$message[1]." ".$message[2]."</a></p>";
}
?>
