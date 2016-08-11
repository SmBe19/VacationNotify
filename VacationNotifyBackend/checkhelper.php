<?php
if(count(get_included_files()) == 1) die("pls don't");
require_once("config.php");
function get_messages(){
  global $CONFIG;
  $messages = [];
  $files = scandir($CONFIG["folder"]);
  foreach($files as $file){
    $matches = [];
    if(preg_match("/".$CONFIG["fileprefix"]."_(\d+)_(\d+)".$CONFIG["filesuffix"]."/", $file, $matches)){
      $message = file_get_contents(sprintf($CONFIG["fullfile"], $matches[1], $matches[2]));
      $messages[] = [$matches[1], $matches[2], $message];
    }
  }
  return $messages;
}
?>
