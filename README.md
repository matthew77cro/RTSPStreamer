# RTP Streamer H264
<p>This project is created for educational purposes only.</p>
<p>By making this RTP H264 Streamer my goal was to learn how H264 codec encodes and decodes video data and also how RTP protocol (used for transfering data with real-time properties) encapsulates and transfers the encoded byte stream.</p>
<p>This project is made in Java from the scratch meaning :</p>
<ul>
<li>This project does not hava real time h264 encoder (data should be encoded beforehand using utility like ffmpeg)</li>
<li>Data should be organized in a&nbsp;particular way so that my code knows how to parse that data : files should be organized using Annex B format and each access unit MUST have AUD - access unit delimiter; also, first NAL unit MUST NOT be AUD (if the encoder places the AUD as the first NAL unit, you should open the file in the hex editor and remove that NAL unit)</li>
<li>Receiving end is not implemented and for that VLC Media Player, ffplay or similar software should be used</li>
</ul>
<p>Also note that this server is not RTSP based, but it is pure RTP server. This means that receiving end will not know what type of data it is getting from this server. Therefore, an .sdp file should be opened on that end so that player knows how to decode that data.</p>
<p>All Java code is documented and comments have been added so that code is more radable.</p>
<p>Here is an example of starting the server:</p>
<ol>
<li>Get video file you want to stream</li>
<li>Using ffmpeg reencode the file as a raw h264 file : <br /><code>fmpeg -i input.mp4 -s 1920x1080 -c:v libx264 -x264opts slice-max-size=1024:aud=1:threads=4:keyint=300:fps=30 -bsf h264_mp4toannexb -an -f h264 output.h264</code></li>
<li>Open output.h264 in a hex editor (recommendation : <a href="https://mh-nexus.de/en/hxd/">HxD</a>)</li>
<li>Remove the first NAL unit if it is AUD (that is, if the first NAL unit is&nbsp;<br /><code>0000 0109 10</code><br />remove it</li>
<li>Start the application:<br /><code>java -cp hr.matija.rtpStreamer.main.Main <em>pathToTheConfigFile</em></code>&nbsp;</li>
<li>Setup the server : setfps, setsrc, setdest... (type "help" for list of all commands, type "man commandName" for help about some command named commandName)</li>
<li>Generate the sdp file from the server command line</li>
<li>Open the sdp file on the receiver end (VLC Media Player, ffplay..)</li>
<li>Type stream in the server command line</li>
</ol>
<p>And that is it! Your stream should now be running... Some screenshots of the process have been included in the Pics directory.</p>
