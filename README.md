# Convert wav format Morse code message to text

This is a solution to [Wundernut vol 11](https://github.com/wunderdogsw/wundernut-vol11).

Expectations: Input file has the follogin format:
```RIFF (little-endian) data, WAVE audio, Microsoft PCM, 16 bit, mono 44100 Hz```

Solution is pretty straightforward. First wav file is read using 44100
sample rate using a [Dynne
library](https://github.com/candera/dynne). Given library gives so
called chunks of each channel (in this case, only one because sample
format is mono). Those chunks are double arrays containing scaled
signal amplitude values.

Using those amplitudes we can down sample the data to have 100 samples
per second instead of 44100. After down sampling we find the amplitude
max for each down sample and decide based on those values if the given
sample is "on" or not. Then we run those down samples through indexed
mapping and find ticks when they do get on and when they get off.

Those ticks provide us enough data to decide whether the signal is dot
or dash. We also can find out letter separators from the same tick
data by changing the comparison off by one.

Morse code specification says letters are separated with duration
equal to 3 dits and words 7. Since we do not know in advance actual
character per minute speed I decided not to decypher dit length at
all. Instead actual signal separations are dynamically measured and
used to differentiate dit, dash and word separator. This means
solution should be able to decode messages with other character per
minute speeds than the example wav.

After that we get dashes and dots and combine that with stream of
letter separators to get the actual Morse code. Morse code is
converted to ASCII format and printed to standard out.

## Compilation

Install Java (developed and tested with Java 11) and
[leiningen](https://leiningen.org/) to build this project.

```lein uberjar```

## Testing

```lein test``

## Running

```java -Djava.awt.headless=true -jar target/wundernut11-morse-decoder.jar message.wav```

## Decoded [message.wav](./message.wav)

```MAY WE TOGETHER BECOME GREATER THAN THE SUM OF BOTH OF US. SAREK.```

Side note: This result is somewhat worrying because it seems it was
Surak who said that in the episode "The Savage Curtain," stardate
5906.4.

# LICENSE

MIT
