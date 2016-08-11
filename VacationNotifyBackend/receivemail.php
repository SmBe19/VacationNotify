<?php
require_once("config.php");
if(defined("STDIN")){
  $message = "";
  while($line = fgets(STDIN)){
    $matches = [];
    if(preg_match("/^subject: (.*)$/i", $line, $matches)){
      $message = $matches[1];
      break;
    }
  }
  if(strlen($message) > 0){
    $filename = sprintf($CONFIG["fullfile"], time(), rand(100000, 999999));
    file_put_contents($filename, $message);
    echo $message;
  } else {
    echo "missing subject";
    return (100);
  }
} else {
  echo "unauthorized";
}
?>
