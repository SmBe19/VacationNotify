<?php
require_once("config.php");
if(isset($_GET["time"]) && isset($_GET["code"])){
  $time = intval($_GET["time"]);
  $code = intval($_GET["code"]);
  $filename = sprintf($CONFIG["fullfile"], $time, $code);
  if(file_exists($filename)){
    unlink($filename);
    echo "done";
  } else {
    echo "unauthorized";
  }
} else {
  echo "unauthorized";
}
?>
