# Convert wav format Morse code message to text

This is a solution to [Wundernut vol 11](https://github.com/wunderdogsw/wundernut-vol11).

Expectations: RIFF (little-endian) data, WAVE audio, Microsoft PCM,
16 bit, mono 44100 Hz describes the input file.

Solution is pretty straightforward. First wav file is read using 44100
sample rate using library. Given library gives so called chunks of
each channel (in this case, only one because mono). Those chunks are
double arrays containing signal amplitudes.

Using those amplitudes we can downs ample the data to have 100 samples
per second instead of 44100. After down sampling we find the amplitude
max for each down sample and decide based on those values if the given
sample is "on" or not. Then we run those mapped downsamples through
mapping and find ticks when they do get on and when they get off.

Those ticks provide us enough data to decide whether the signal is dot
or dash. We also can find out letter separators from the same tick
data by changing the comparison off by one.

After that we get dashes and dots and combine that with stream of word
separators to get the actual Morse code. Then that code is decoded to
ascii.

## Decoded [message.wav](./message.wav)

"MAYWETOGETHERBECOMEGREATERTHANTHESUMOFBOTHOFUS.SAREK."

This is somewhat odd because it seems it was Surak who said that in
the episode "The Savage Curtain," stardate 5906.4.

# LICENSE

MIT
