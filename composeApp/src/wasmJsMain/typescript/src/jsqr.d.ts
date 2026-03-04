declare module "jsqr" {
  interface QRCode {
    data: string;
  }
  function jsQR(
    data: Uint8ClampedArray,
    width: number,
    height: number,
  ): QRCode | null;
  export default jsQR;
}
