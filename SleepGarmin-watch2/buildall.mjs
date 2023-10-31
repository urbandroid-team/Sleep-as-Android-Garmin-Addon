// # read manifest iq:product

// java -Xms1g -Dfile.encoding=UTF-8 -Dapple.awt.UIElement=true -jar /home/artaud/.Garmin/ConnectIQ/Sdks/connectiq-sdk-lin-4.0.7-2021-11-29-437ff4cc4/bin/monkeybrains.jar -o /home/artaud/Projects/urbandroid/SleepGarmin/SleepGarmin-watch2/build/20220118.0.0/SleepGarmin-watch2.prg -f /home/artaud/Projects/urbandroid/SleepGarmin/SleepGarmin-watch2/monkey.jungle -y "/home/artaud/Insync/richtej2@gmail.com/Google Drive/keystore/urbandroid/garmin_developer_key.der" -w -d venu -r

import parser from "xml2json";
import fs from "fs";
import {exec} from "child_process";


fs.readFile( './manifest.xml', function(err, data) {
  const manifest = JSON.parse(parser.toJson(data));
  // console.log("to json ->", manifest);

  const devices = manifest['iq:manifest']['iq:application']['iq:products']['iq:product']

  // console.log(devices)
  for (const d of devices) {
    const model = d.id

    const command = `java -Xms1g -Dfile.encoding=UTF-8 -Dapple.awt.UIElement=true -jar /home/petr/.Garmin/ConnectIQ/Sdks/connectiq-sdk-lin-4.2.4-2023-04-05-5830cc591/bin/monkeybrains.jar -o /home/petr/project/android-git/SleepAsGarmin/Sleep-as-Android-Garmin-Addon/SleepGarmin-watch2/build/20220118.0.0/sleep-${model}.prg -f /home/petr/project/android-git/SleepAsGarmin/Sleep-as-Android-Garmin-Addon/SleepGarmin-watch2/monkey.jungle -y "//home/petr/project/android-git/SleepAsGarmin/Sleep-as-Android-Garmin-Addon/SleepGarmin-watch2/garmin_developer_key.der" -w -d ${model} -r`

    exec(command, (err, stdout, stderr) => {
      if (err) {
        console.log(`err: ${err.message}`);
        return;
      }
      if (stderr) {
        console.log(`stderr: ${stderr}`);
        return;
      }
      console.log(`${stdout}`);
      })
  }

 });
