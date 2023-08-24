# Sleep-as-Android-Garmin-Addon

[![Join the chat at https://gitter.im/Sleep-as-Android-Garmin-Addon/Lobby](https://badges.gitter.im/Sleep-as-Android-Garmin-Addon/Lobby.svg)](https://gitter.im/Sleep-as-Android-Garmin-Addon/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This is a full source for the Garmin watch integration for Sleep as Android by Urbandroid Team. The repo contains both Android part and Garmin part. Contributions are welcome as well as any inquiries!

## Android addon

Android part (distributed on Play Store as [Sleep as Android Garmin Addon](https://play.google.com/store/apps/details?id=com.urbandroid.sleep.garmin)) can be found under /SleepGarmin-android.

## Garmin watch app

There are two versions of the Garmin watch app:

- the old app (distributed until Aug 2020) can be found under /SleepGarmin-watch
- the new app after a complete rewrite, with min CIQ API level 3.1 and using batched sensor readings, distributed since Aug 2020 on Garmin ConnectIQ store as [Sleep as Android](https://apps.garmin.com/cs-CZ/apps/e80a4793-f5a3-44c7-bd7f-52a97f5d8310)) can be found under /SleepGarmin-watch2.

### Releases and sideloading

Release versions for the watch app can be found under /watch-releases. Each version is an .iq file, which is actually a ZIP archive containing versions of the app for all compatible devices.

To sideload a specific version of the app onto your watch:
- unzip the .iq file
- open up the `manifest.xml` file. Among other things you'll see a list of devices, each one looking like this: 
```xml
<iq:product connectIqVersion="3.3.1" filename="006-B3226-00/SleepGarmin-watch2.prg" id="venu" minFirmwareVersion="680" partNumber="006-B3226-00" sig="5B3539883AA107685C205378BBC25E70A4390845C7B3663324CB4E03D30A773BDB1E674EB4664290EB5316C9A6F717F3F293AEE9A3B39DDA489C6EC04B90CA5B5514357136E9F720807D10DA38E7F133E3AD053E241E43C562BA99B5AE942FF47198A456D5F44BC303A4B6101A2F86535C09904F4AEF2BF939D41D9BB13337E7DB34AE4A82FF7853E6AD66483E14A301D972EFA95619859DA23A021516DE12CE9B59F06010B86D61407014A529CFF8D4CEEFE6CADEA46C921CEB252136C66E07CE56E38FEBFE35B88908096FB9049550A6E56A4C1ADE7FAE34150FCEE83207045EC249AE976B8748C882533D40908AF11472B53198E0DE822F0C0528B35C4765752100FA52FAC5946C0DC6854A975BD4D50526AEB730FE1916D01E1B9B419DE63327110FE0A589741EF46E2C79AA6CA9395B49B5D241D6112721DEA542F4F144FD76E34931992D512BEE7C05324F3483A864B71556DCAD14ECECE3A2EDBFABD34427E00F7F93050496DBC6B504F042AA7C1A6073796F45406E53FF9FE2FA8346B96067E849B71A103E4A23C392AE6302B06A1DEFBC078D41F5FDAB9EF57BA24BB99881960DACCD276CF9E6FCB6310AEE89F1813F8256AC78658F2647E701C62B0253CB042BDDA9B72E331345B45785966710F70714DCE48F8A0FEE48BD590C0EF71ADE2FEC2E61FAA2227E770528A130237C1895C7667FCF538F83B891177B60" sigFormat="rsa-4096"/>
```
- take a look at `id` - that's the model of the device, and `filename` shows the corresponding file to be sideloaded
- find your device in the list and get its .prg file
- take this .prg file and copy it onto your device, into /GARMIN/APPS (make sure to delete any previously installed or sideloaded version of the same app)

## Responsible developer

[Jiri Richter](mailto:jiri.richter@urbandroid.org)