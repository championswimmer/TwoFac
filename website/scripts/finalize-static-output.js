import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const websiteDir = path.resolve(__dirname, '..')
const distDir = path.join(websiteDir, 'dist')
const generated404File = path.join(distDir, '404', 'index.html')
const existing404File = path.join(distDir, '404.html')
const fallbackFile = path.join(distDir, 'index.html')
const output404File = path.join(distDir, '404.html')

if (fs.existsSync(existing404File)) {
  console.log(`Keeping existing ${existing404File}`)
}
else {
  const sourceFile = fs.existsSync(generated404File) ? generated404File : fallbackFile

  if (!fs.existsSync(sourceFile)) {
    throw new Error(`Could not find a source HTML file to create ${output404File}`)
  }

  fs.copyFileSync(sourceFile, output404File)
  console.log(`Created ${output404File} from ${sourceFile}`)
}
