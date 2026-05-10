# JPEG2000 / JP2 Decoder

This package contains Ptolemy3D's custom JPEG2000/JP2 tile decoder. It is a core part of the project's value because the viewer was designed around tiled JP2 imagery rather than common web image tile formats.

## Important classes

| Class | Role |
| --- | --- |
| `Jp2Head` | Parses and stores JP2/J2K header metadata used by tile decoding. |
| `Jp2Block` | Stores tile packet/code-block offsets, lengths, inclusion flags, and packet metadata. |
| `Jp2Decoder` | Decodes a tile at a requested resolution and reconstructs component data. |
| `EntropyDecoder` | Decodes JPEG2000 entropy-coded code-block data. |
| `Subband` | Represents the wavelet subband tree and per-subband geometry/quantization metadata. |
| `ByteArrayReader` | Byte-level reader used by JP2 parsing code. |
| `PktHeaderBitReader` | Bit-level packet-header reader. |
| `TagTreeDecoder` | JPEG2000 tag-tree decoder helper. |

## Decoder flow

The intended flow is:

1. Parse a representative/default JP2 header with `Jp2Head`.
2. Parse per-tile code-block/packet metadata into `Jp2Block`.
3. Use `Jp2Decoder` to decode the requested tile resolution.
4. Traverse the wavelet subband tree for the target resolution.
5. Decode included code blocks through `EntropyDecoder`.
6. Dequantize and reconstruct component arrays.
7. Convert component arrays into RGB texture bytes through the tile pipeline.

## Resolution behavior

The decoder is resolution-aware. Resolution level `0` uses the LL band directly. Higher resolution levels decode LH, HL, and HH subbands and perform wavelet reconstruction against the lower-resolution data. This supports progressive tile display where coarse imagery can appear before higher-detail imagery is available.

## Scope and limitations

This decoder appears tailored to the JP2/J2K structure expected by Ptolemy3D's tiled map data. Do not assume it is a complete general-purpose JPEG2000 implementation without validating it against your target files.

When modernizing the project, treat this package as valuable reference code and build tests around known-good JP2 tiles before refactoring it.

## Extraction target

A useful standalone API would look like:

```text
decodeJp2Tile(headerBytes, tileBytes, resolution) -> RGB texture bytes + width + height
```

That API should be independent of JOGL, applets, network loading, and global `Ptolemy3D` runtime state.
