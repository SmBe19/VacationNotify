<?php
require_once("config.php");
require_once("checkhelper.php");
$messages = get_messages();
foreach($messages as $message){
  echo $message[0]." ".$message[1]." ".$message[2]."\n";
}
?>
