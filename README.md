# RTSP Streamer H264
<p>This project is created for educational purposes only.</p>
<p>By making this RTSP H264 Streamer my goal was to learn how H264 codec encodes and decodes video data and also how RTP protocol (used for transfering data with real-time properties) encapsulates and transfers the encoded byte stream.</p>
<p>This project is made in Java from the scratch meaning :</p>
<ul>
<li>This project does not hava real time h264 encoder (data should be encoded beforehand using utility like ffmpeg)</li>
<li>Data should be organized in a&nbsp;particular way so that my code knows how to parse that data : files should be organized using Annex B format and each access unit MUST have AUD - access unit delimiter; also, first NAL unit MUST NOT be AUD (if the encoder places the AUD as the first NAL unit, you should open the file in the hex editor and remove that NAL unit)</li>
<li>Receiving end is not implemented and for that VLC Media Player, ffplay or similar software should be used</li>
</ul>
<p><strike>Also note that this server is not RTSP based, but it is pure RTP server. This means that receiving end will not know what type of data it is getting from this server. Therefore, an .sdp file should be opened on that end so that player knows how to decode that data.</strike></p>
<p>RTSP has been implemented! It is now easier than ever to connect to this server!&nbsp;</p>
<p>All Java code is documented and comments have been added so that code is more radable.</p>
<p>Here is an example of starting the server:</p>
<ol>
<li>Get video file you want to stream</li>
<li>Using ffmpeg reencode the file as a raw h264 file : <br /><code>fmpeg -i input.mp4 -s 1920x1080 -c:v libx264 -x264opts slice-max-size=1024:aud=1:threads=4:keyint=300:fps=30 -bsf h264_mp4toannexb -an -f h264 output.h264</code></li>
<li>Open output.h264 in a hex editor (recommendation : <a href="https://mh-nexus.de/en/hxd/">HxD</a>)</li>
<li>Remove the first NAL unit if it is AUD (that is, if the first NAL unit is&nbsp;<br /><code>0000 0109 10</code><br />remove it)</li>
<li>Setup the server : create the config file (server.properties file) and resource descriptor file. Examples and documentation of these two files are in the webroot directory</li>
<li>Start the server:<br /><code>java -cp hr.matija.rtpStreamer.main.Main <em>pathToTheConfigFile</em></code>&nbsp;</li>
<li>Start the client side application (VLC Media Player, ffplay..)</li>
<li>Connect to the server with "rtsp://hostname:port/resource_mapping"</li>
</ol>
<p>And that is it! Your stream should now be running...</p> <br>
<p> <strong>Heads-up</strong> about using windows and VLC for client side video receiving : 
  <ul>
  <li> Windows Firewall could be a massive problem, so if server says that it got request it cannot understand and/or VLC does not even start to play the video, try <strong>disabling Windows Firewall on client side</strong> and/or server side </li>
  <li> If video appears to be "blocky" and/or is lagging, try disabling <strong>hardware accelerated decoding</strong> in VLC and setting the H264 parameters to <strong>zerolatency</strong> </li>
    </ul></p> <br>
<p>Useful links: <br /> <a href="https://tools.ietf.org/html/rfc768">UDP Specification</a> <br /> <a href="https://tools.ietf.org/html/rfc3550">RTP Specification</a> <br /> <a href="https://tools.ietf.org/html/rfc6184#ref-1">RTP payload for H.264 specifiction</a> <br /> <a href="https://www.quora.com/What-is-the-difference-between-an-I-Frame-and-a-Keyframe-in-video-encoding">Kay frame explaination</a> <br /> <a href="https://yumichan.net/video-processing/video-compression/introduction-to-h264-nal-unit/">H.264 nal unit specification</a> <br /> <a href="https://stackoverflow.com/questions/22626021/idr-and-non-idr-difference">IDR frames</a> <br /> <a href="https://en.wikipedia.org/wiki/Network_Abstraction_Layer">NAL Units Wiki page</a> <br /> <a href="https://cardinalpeak.com/blog/worlds-smallest-h-264-encoder/">World's samllest h.264 encoder</a> <br /> <a href="https://cardinalpeak.com/blog/the-h-264-sequence-parameter-set/">H.264 SPS</a> <br /> <a href="https://en.wikipedia.org/wiki/Real_Time_Streaming_Protocol">RTSP Wiki</a> <br /> <a href="https://tools.ietf.org/html/rfc7826">RTSP Specification</a> <br /> <a href="https://en.wikipedia.org/wiki/Uniform_Resource_Identifier">URI Wiki</a></p>
